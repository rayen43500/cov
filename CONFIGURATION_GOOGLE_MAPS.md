# 🗺️ Guide de Configuration Google Maps

## Problème : Google Maps ne fonctionne pas

Si Google Maps ne s'affiche pas dans votre application, suivez ces étapes :

## ✅ Étapes de Configuration

### 1. Obtenir une Clé API Google Maps

1. **Aller sur Google Cloud Console**
   - Visitez : https://console.cloud.google.com/
   - Connectez-vous avec votre compte Google

2. **Créer ou Sélectionner un Projet**
   - Cliquez sur le sélecteur de projet en haut
   - Cliquez sur "Nouveau projet" ou sélectionnez un projet existant

3. **Activer l'API Maps SDK for Android**
   - Dans le menu latéral, allez dans "APIs & Services" > "Library"
   - Recherchez "Maps SDK for Android"
   - Cliquez dessus et cliquez sur "Enable"

4. **Créer une Clé API**
   - Allez dans "APIs & Services" > "Credentials"
   - Cliquez sur "Create Credentials" > "API Key"
   - Une clé API sera générée

5. **Restreindre la Clé API (Recommandé)**
   - Cliquez sur la clé API créée
   - Dans "Application restrictions", sélectionnez "Android apps"
   - Cliquez sur "Add an item"
   - Entrez le nom du package : `com.example.rideshare1`
   - Obtenez le SHA-1 de votre application :
     ```bash
     # Sur Windows (PowerShell)
     cd android
     .\gradlew signingReport
     ```
     Cherchez "SHA1" dans la sortie
   - Entrez le SHA-1 fingerprint
   - Dans "API restrictions", sélectionnez "Restrict key"
   - Cochez "Maps SDK for Android"
   - Cliquez sur "Save"

### 2. Configurer la Clé API dans l'Application

1. **Ouvrir AndroidManifest.xml**
   - Fichier : `app/src/main/AndroidManifest.xml`

2. **Remplacer la Clé API**
   - Trouvez cette ligne (ligne 98) :
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="YOUR_GOOGLE_MAPS_API_KEY" />
     ```
   - Remplacez `YOUR_GOOGLE_MAPS_API_KEY` par votre vraie clé API
   - Exemple :
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" />
     ```

### 3. Vérifier les Permissions

Assurez-vous que ces permissions sont dans `AndroidManifest.xml` :
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 4. Vérifier les Dépendances

Dans `app/build.gradle.kts`, assurez-vous d'avoir :
```kotlin
// Google Maps
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.android.gms:play-services-location:21.0.1")
```

### 5. Synchroniser et Rebuild

1. Dans Android Studio :
   - File > Sync Project with Gradle Files
   - Build > Clean Project
   - Build > Rebuild Project

2. Redémarrer l'application

## 🔍 Dépannage

### Erreur : "Google Maps API key not found"
- ✅ Vérifiez que la clé API est bien dans `AndroidManifest.xml`
- ✅ Vérifiez qu'il n'y a pas d'espaces avant/après la clé
- ✅ Vérifiez que la clé est entre guillemets

### Erreur : "This API project is not authorized"
- ✅ Vérifiez que l'API "Maps SDK for Android" est activée
- ✅ Vérifiez que la clé API n'est pas restreinte de manière incorrecte

### La carte est blanche
- ✅ Vérifiez votre connexion Internet
- ✅ Vérifiez que la clé API est valide
- ✅ Vérifiez les logs Android pour les erreurs :
  ```
  adb logcat | grep -i "maps\|google"
  ```

### La carte ne charge pas les marqueurs
- ✅ Vérifiez que les coordonnées GPS sont valides (non nulles)
- ✅ L'application utilise maintenant le Geocoding pour obtenir les coordonnées automatiquement

## 📝 Notes Importantes

1. **Clé API Gratuite** : Google Maps offre un crédit gratuit mensuel (généralement suffisant pour le développement)

2. **Sécurité** : Ne commitez JAMAIS votre clé API dans Git si elle n'est pas restreinte

3. **Test** : Testez toujours sur un appareil réel ou un émulateur avec Google Play Services installé

4. **Coordonnées GPS** : L'application peut maintenant obtenir automatiquement les coordonnées GPS à partir des noms de villes grâce au Geocoding

## ✅ Vérification Finale

Après configuration, vous devriez voir :
- ✅ La carte Google Maps s'affiche
- ✅ Les marqueurs de départ et d'arrivée apparaissent
- ✅ Une ligne bleue connecte les deux points
- ✅ La carte se centre automatiquement sur le trajet

## 🆘 Besoin d'Aide ?

Si le problème persiste :
1. Vérifiez les logs Android Studio (Logcat)
2. Vérifiez que Google Play Services est à jour sur votre appareil
3. Testez avec une clé API non restreinte temporairement pour isoler le problème

