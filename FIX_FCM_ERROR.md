# Guide de résolution de l'erreur 400 FCM (Firebase Cloud Messaging)

## 🔍 Problème

Vous rencontrez une erreur HTTP 400 lors de l'authentification Google Play Services pour Firebase Cloud Messaging :

```
400 POST https://android.googleapis.com/auth/devicekey
```

Cette erreur est généralement **non bloquante** et n'empêche pas l'application de fonctionner. Elle est liée à l'enregistrement du device pour recevoir les notifications push.

## ✅ Solutions

### Solution 1 : Utiliser un appareil physique (Recommandé)

Les notifications FCM fonctionnent mieux sur un appareil physique :

1. **Connecter un appareil Android réel** via USB
2. **Activer le mode développeur** sur l'appareil
3. **Activer le débogage USB**
4. **Exécuter l'application** sur l'appareil physique

### Solution 2 : Configurer l'émulateur avec Google Play Services

Si vous devez utiliser un émulateur :

1. **Créer un nouvel émulateur** avec Google Play Services :
   - Dans Android Studio : Tools > Device Manager
   - Créer un nouvel appareil virtuel (AVD)
   - **IMPORTANT** : Choisir une image système avec l'icône "Play Store" (Google Play)
   - Exemples : Pixel 5 avec API 30+ (Google Play)

2. **Mettre à jour Google Play Services** :
   - Ouvrir Play Store sur l'émulateur
   - Mettre à jour Google Play Services
   - Redémarrer l'émulateur

### Solution 3 : Ajouter la clé SHA-1 dans Firebase Console

1. **Obtenir la clé SHA-1** :
   ```bash
   # Windows (PowerShell)
   cd android
   .\gradlew signingReport
   
   # Linux/Mac
   cd android
   ./gradlew signingReport
   ```

2. **Copier la clé SHA-1** (Debug et Release)

3. **Ajouter dans Firebase Console** :
   - Aller sur [Firebase Console](https://console.firebase.google.com)
   - Sélectionner votre projet
   - Paramètres du projet (⚙️) > Vos applications
   - Cliquer sur votre application Android
   - Ajouter l'empreinte digitale (SHA-1)
   - Télécharger le nouveau `google-services.json`
   - Remplacer le fichier dans `app/google-services.json`

### Solution 4 : Ignorer l'erreur (si non bloquante)

Cette erreur n'empêche généralement pas l'application de fonctionner. Les fonctionnalités principales (authentification, Firestore, etc.) continuent de fonctionner.

Pour ignorer l'erreur dans les logs :

1. L'erreur apparaît dans Logcat mais n'affecte pas l'application
2. Les notifications push peuvent ne pas fonctionner sur l'émulateur
3. Tester sur un appareil physique pour les notifications

### Solution 5 : Vérifier la configuration Firebase

1. **Vérifier que FCM est activé** :
   - Firebase Console > Cloud Messaging
   - Vérifier que l'API est activée

2. **Vérifier les permissions** :
   - `AndroidManifest.xml` doit contenir :
     ```xml
     <uses-permission android:name="android.permission.INTERNET" />
     ```

3. **Vérifier le service FCM** :
   - `AndroidManifest.xml` doit contenir :
     ```xml
     <service
         android:name=".Services.FCMService"
         android:exported="false">
         <intent-filter>
             <action android:name="com.google.firebase.MESSAGING_EVENT" />
         </intent-filter>
     </service>
     ```

## 🔧 Vérifications

### Vérifier que Firebase est correctement initialisé

L'application devrait initialiser Firebase automatiquement via `google-services.json`. Vérifiez que :

1. Le fichier `google-services.json` est dans `app/`
2. Le plugin Google Services est dans `build.gradle.kts` :
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
   }
   ```

### Vérifier les logs

Dans Logcat, recherchez :
- `FCMService` : Logs du service FCM
- `FirebaseMessaging` : Logs de Firebase Messaging
- `GooglePlayServices` : Erreurs Google Play Services

## 📝 Notes importantes

1. **L'erreur 400 sur `/auth/devicekey` est courante** sur les émulateurs sans Google Play Services
2. **Elle n'empêche pas l'authentification** Firebase de fonctionner
3. **Les notifications push peuvent ne pas fonctionner** sur l'émulateur
4. **Tester sur un appareil physique** pour une expérience complète

## 🚀 Test rapide

Pour vérifier si l'authentification fonctionne malgré l'erreur :

1. Lancer l'application
2. Essayer de se connecter
3. Si la connexion fonctionne, l'erreur FCM n'est pas bloquante
4. Les notifications push peuvent être testées plus tard sur un appareil physique

## 📞 Support supplémentaire

Si le problème persiste :

1. Vérifier la version de Google Play Services sur l'émulateur
2. Créer un nouvel émulateur avec Google Play
3. Utiliser un appareil physique pour les tests finaux
4. Consulter la [documentation Firebase](https://firebase.google.com/docs/cloud-messaging/android/client)

---

**Note** : Cette erreur est généralement **cosmétique** et n'affecte pas les fonctionnalités principales de l'application. L'authentification, Firestore, et les autres services Firebase continuent de fonctionner normalement.

