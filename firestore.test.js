const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-no-project",
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write to vault", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  const vaultRef = unauthedDb.collection("users").doc("alice").collection("vault_items").doc("item1");
  await assertFails(vaultRef.get());
  await assertFails(vaultRef.set({
    userId: "alice",
    title: "Test Secret",
    category: "CREDENTIAL",
    encryptedPayload: "dGVzdA==",
    securityLevel: "HIGH",
    iv: "aXY=",
    favorite: false,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user can write to their own vault with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const vaultRef = aliceDb.collection("users").doc("alice").collection("vault_items").doc("item1");
  await assertSucceeds(vaultRef.set({
    userId: "alice",
    title: "AWS Access Key",
    category: "API_KEY",
    encryptedPayload: "T1h4V2VjcmV0S2V5UGF5bG9hZA==",
    securityLevel: "MAXIMUM",
    iv: "ZXhhbXBsZUl2MTI=",
    favorite: true,
    tags: ["cloud", "prod"],
    notesPreview: "Production AWS key",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user cannot access another user's vault items", async () => {
  const bobDb = testEnv.authenticatedContext("bob").firestore();
  const aliceVaultRef = bobDb.collection("users").doc("alice").collection("vault_items").doc("item1");
  await assertFails(aliceVaultRef.get());
  await assertFails(aliceVaultRef.set({
    userId: "alice",
    title: "Bob Trying to Write Alice's Secret",
    category: "CREDENTIAL",
    encryptedPayload: "aGFja2Vk",
    securityLevel: "STANDARD",
    iv: "aXY=",
    favorite: false,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Audit logs are immutable: creation succeeds, update and delete fail", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const logRef = aliceDb.collection("users").doc("alice").collection("audit_logs").doc("log1");
  await assertSucceeds(logRef.set({
    userId: "alice",
    eventType: "ITEM_ENCRYPTED",
    severity: "INFO",
    details: "Vault item encrypted with AES-256",
    ipAddress: "192.168.1.100",
    createdAt: new Date()
  }));

  // Update must fail (immutable)
  await assertFails(logRef.update({ details: "Tampered log" }));
  // Delete must fail (immutable)
  await assertFails(logRef.delete());
});

test("Security profile can be read and updated by owner", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const profileRef = aliceDb.collection("users").doc("alice").collection("security_profile").doc("main");
  await assertSucceeds(profileRef.set({
    userId: "alice",
    securityScore: 95,
    twoFactorEnabled: true,
    biometricLockEnabled: true,
    autoLockSeconds: 60,
    backendStatus: "HEALTHY",
    updatedAt: new Date()
  }));
  await assertSucceeds(profileRef.get());
});
