# Configuration de Gemini AI Chatbot

## 📋 Instructions pour configurer le chatbot Gemini

### 1. Obtenir une clé API Gemini (Gratuite)

1. Allez sur [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Connectez-vous avec votre compte Google
3. Cliquez sur "Create API Key"
4. Copiez votre clé API

### 2. Configurer la clé API dans l'application (sans la commiter)

L'app lit la clé via `BuildConfig.GEMINI_API_KEY` qui est injectée au build.  
Choisissez l'une des options :

- **Variable d'environnement** (recommandé)  
  - PowerShell : `$Env:GEMINI_API_KEY="votre_cle"`  
  - Bash : `export GEMINI_API_KEY="votre_cle"`

- **Propriété Gradle locale (non suivie)**  
  Ajoutez dans `local.properties` (fichier non versionné) :  
  ```
  GEMINI_API_KEY=votre_cle
  ```

> Ne mettez pas la clé dans les fichiers Java/Kotlin ni dans `gradle.properties` suivi par Git.

### 3. Modèle utilisé

- **Modèle** : `gemini-2.5-flash` (Gemini Flash 2.5)
- **Limite indicative** : dépend de votre quota (free ou payant) et de votre projet

### 4. Fonctionnalités

Le chatbot est disponible uniquement pour les **passagers** et permet de :
- Poser des questions sur les trajets
- Demander des informations sur les places disponibles
- Obtenir des conseils sur les prix
- Demander des informations sur les horaires et destinations

### 5. Accès

Le chatbot est accessible depuis :
- Le menu de navigation en bas de l'écran (icône "Assistant IA")
- Uniquement dans l'espace passager (`PassengerMainActivity`)

### 6. Sécurité

⚠️ **Important** : Ne commitez jamais votre clé API dans un dépôt public. Utilisez des variables d'environnement ou un fichier de configuration local non versionné.

### 7. Dépannage

Si le chatbot ne fonctionne pas :
1. Vérifiez que votre clé API est correctement configurée
2. Vérifiez votre connexion Internet
3. Vérifiez que vous n'avez pas dépassé la limite de requêtes (15 RPM)
4. Consultez les logs dans Logcat pour plus de détails

