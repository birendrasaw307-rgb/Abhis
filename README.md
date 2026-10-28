# SecurePlus - Decoupled Frontend Application 🛡️

**SecurePlus** एक आधुनिक, उच्च-सुरक्षा (Security+) एंड-टू-एंड एन्क्रिप्टेड वॉल्ट और क्रेडेंशियल मैनेजर एंड्रॉइड ऐप्लिकेशन है। 

इस रिपॉजिटरी में एप्लिकेशन का **संपूर्ण फ्रंटएंड (Android / Jetpack Compose)** स्वतंत्र (decoupled) रूप से व्यवस्थित किया गया है, जिसे आप सीधे अपने **GitHub** अकाउंट पर पुश कर सकते हैं।

---

## 🏗️ Decoupled Architecture (अलग बैकएंड एवं अलग फ्रंटएंड)

यह ऐप्लिकेशन **Clean MVVM Architecture** का पालन करता है, जहाँ फ्रंटएंड और बैकएंड एक-दूसरे से पूरी तरह अलग (Decoupled) हैं:

```
┌─────────────────────────────────────────────────────────────┐
│                 FRONTEND (This Repository)                  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │           Jetpack Compose M3 UI Layer               │   │
│   │  • Security Hub (डैशबोर्ड & हेल्थ स्कोर)             │   │
│   │  • Cloud Vault (एन्क्रिप्टेड सीक्रेट्स सूची)        │   │
│   │  • Backend Monitor (लाइव लेटेंसी & रूल्स टेलीमेट्री)│   │
│   │  • Audit Trail (सुरक्षा लॉग्स स्ट्रीम)              │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              │                              │
│   ┌──────────────────────────▼──────────────────────────┐   │
│   │          Client Cryptography Engine                 │   │
│   │  • AES-256-GCM + PBKDF2 Key Derivation              │   │
│   │  • In-Memory Master PIN Session Protection          │   │
│   │  • Password Entropy Analyzer & Generator            │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              │                              │
│   ┌──────────────────────────▼──────────────────────────┐   │
│   │          Decoupled Repository / Data Layer          │   │
│   │  • VaultRepository                                  │   │
│   │  • AuditRepository                                  │   │
│   │  • BackendMonitorRepository                         │   │
│   └──────────────────────────┬──────────────────────────┘   │
└──────────────────────────────┼──────────────────────────────┘
                               │ Authenticated Zero-Knowledge Payloads
                               ▼ (Only Ciphertext Sent)
┌─────────────────────────────────────────────────────────────┐
│                 BACKEND (Cloud Services)                    │
│                                                             │
│   • Google Cloud Firestore (`users/{userId}/...`)           │
│   • Google Identity / Firebase Auth                         │
│   • Server-Side Zero-Trust Security Rules                   │
│   • Immutable Audit Event Storage                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 GitHub पर कैसे पुश करें (How to Push to GitHub)

### **विकल्प 1: Git कमांड लाइन द्वारा (Recommended)**

यदि आपने GitHub पर एक नई खाली रिपॉजिटरी (उदा. `secureplus-frontend`) बनाई है:

```bash
# 1. अपने GitHub रिमोट यूआरएल को जोड़ें
git remote add origin https://github.com/<YOUR-USERNAME>/<YOUR-REPO-NAME>.git

# 2. मेन ब्रांच सेट करें
git branch -M main

# 3. गिटहब पर कोड पुश करें
git push -u origin main
```

> **सुझाव**: आप हमारे द्वारा तैयार किए गए ऑटोमेशन स्क्रिप्ट का भी उपयोग कर सकते हैं:
> ```bash
> chmod +x push_to_github.sh
> ./push_to_github.sh https://github.com/<YOUR-USERNAME>/<YOUR-REPO-NAME>.git
> ```

---

### **विकल्प 2: AI Studio UI द्वारा सीधे पुश करें**

1. ऊपर दाईं ओर स्थित **Settings** या **Share / Export** मेनू पर जाएँ।
2. **"Push to GitHub"** चुनें।
3. अपने GitHub अकाउंट को कनेक्ट करके अपनी रिपॉजिटरी में सीधे पुश करें।

---

## 📁 फ्रंटएंड डायरेक्टरी संरचना (Directory Structure)

```
app/src/main/java/com/example/
├── MainActivity.kt               # ऐप का रूट एंट्री पॉइंट और Auth Gate
├── ui/
│   ├── auth/
│   │   ├── AuthManager.kt       # Google Sign-In Credential Manager
│   │   └── AuthScreen.kt        # आधुनिक प्रमाणीकरण स्क्रीन
│   ├── screens/
│   │   ├── SecurityHubScreen.kt # 4-लेयर डिफेंस और सुरक्षा स्कोर
│   │   ├── VaultScreen.kt       # सुरक्षित तिजोरी, सर्च और डिक्रिप्शन
│   │   ├── BackendMonitorScreen.kt # अलग बैकएंड मॉनिटर और लेटेंसी पिंग
│   │   ├── AuditLogScreen.kt    # इम्यूटेबल ऑडिट लॉग टाइमलाइन
│   │   ├── AddEditSecretDialog.kt # नया सीक्रेट एन्क्रिप्ट करने का डायलॉग
│   │   └── MasterPinDialog.kt   # मास्टर पिन अनलॉक डायलॉग
│   ├── theme/
│   │   ├── Color.kt             # साइबरसिक्योरिटी एमराल्ड/सायन पैलेट
│   │   ├── Theme.kt             # मटेरियल 3 डार्क थीम
│   │   └── Type.kt              # टाइपोग्राफी
│   └── viewmodel/
│       └── SecureVaultViewModel.kt # रिएक्टिव बिज़नेस लॉजिक एवं स्टेटफ्लो
└── data/
    ├── error/
    │   └── FirestoreErrorHandler.kt # स्ट्रक्चर्ड एरर हैंडलिंग
    ├── model/
    │   └── VaultModels.kt       # डेटा क्लास मॉडल्स
    ├── repository/
    │   ├── VaultRepository.kt   # डेटाबेस रिपॉजिटरी
    │   ├── AuditRepository.kt   # ऑडिट लॉग रिपॉजिटरी
    │   └── BackendMonitorRepository.kt # टेलीमेट्री रिपॉजिटरी
    └── security/
        ├── CryptoManager.kt     # AES-256-GCM एन्क्रिप्शन इंजन
        └── SecurityAnalyzer.kt  # पासवर्ड स्ट्रेंथ और एंट्रॉपी एनालाइज़र
```

---

## 🛠️ बिल्ड और टेस्ट कमांड्स (Build & Test)

```bash
# डिबग एपीके (APK) बिल्ड करें
gradle :app:assembleDebug

# यूनिट और रोबोलेक्ट्रिक टेस्ट्स चलाएँ
gradle :app:testDebugUnitTest
```

---

## 🔒 सुरक्षा विशेषताएँ (Security Highlights)

1. **शून्य-ज्ञान एन्क्रिप्शन (Zero-Knowledge)**: कोई भी प्लेनटेक्स्ट सीक्रेट कभी भी नेटवर्क पर या बैकएंड को नहीं भेजा जाता। डेटा डिवाइस के अंदर AES-256-GCM द्वारा एन्क्रिप्ट होने के बाद ही जाता है।
2. **मास्टर पिन**: डिफ़ॉल्ट मास्टर पिन `1234` है, जिसे यूज़र अपनी पसंद से कभी भी बदल सकता है।
3. **सख्त ऑथेंटिकेशन**: बिना Google Sign-In प्रमाणीकरण के बैकएंड का कोई भी डेटा एक्सेस नहीं किया जा सकता।
