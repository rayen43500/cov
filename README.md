# RideShare - Application de Covoiturage

Application Android de covoiturage développée en Java avec architecture MVVM et Firebase.

## 📋 Description

RideShare est une application mobile Android permettant la mise en relation entre conducteurs et passagers pour des trajets de covoiturage. L'application offre une expérience complète avec authentification, gestion de trajets, réservations, messagerie instantanée, intégration Google Maps et système d'évaluation.

## 🏗️ Architecture

- **Architecture**: MVVM (Model-View-ViewModel)
- **Langage**: Java
- **Backend**: Firebase (Authentication, Firestore, Storage, Cloud Messaging)
- **Maps**: Google Maps API
- **UI**: Material Design Components

## ✨ Fonctionnalités

### Sprint 1: Authentification
- Création de compte (Passager/Conducteur)
- Connexion sécurisée
- Gestion de session
- Récupération de mot de passe

### Sprint 2: Gestion des trajets
- Publication de trajets (Conducteur)
- Recherche de trajets (Passager)
- Réservation de trajets
- Gestion des réservations (Accepter/Refuser)
- Notifications

### Sprint 3: Gestion avancée
- Modification du profil
- Gestion de la photo de profil
- Suppression de compte
- Système d'évaluation (notation + commentaires)
- Consultation des avis

### Sprint 4: Fonctionnalités avancées
- Messagerie instantanée
- Intégration Google Maps
- Historique des trajets
- Notifications enrichies

## 🚀 Configuration

### Prérequis
- Android Studio (Giraffe/Iguana ou version récente)
- JDK 8 ou 11
- Compte Firebase
- Clé API Google Maps

### Installation

1. **Cloner le projet**
   ```bash
   git clone <repository-url>
   cd rideshare1
   ```

2. **Configurer Firebase**
   - Créer un projet Firebase sur [Firebase Console](https://console.firebase.google.com)
   - Télécharger le fichier `google-services.json`
   - Remplacer le fichier `app/google-services.json` par votre fichier

3. **Configurer Google Maps**
   - Obtenir une clé API Google Maps sur [Google Cloud Console](https://console.cloud.google.com)
   - Remplacer `YOUR_GOOGLE_MAPS_API_KEY` dans `AndroidManifest.xml` par votre clé API

4. **Synchroniser les dépendances**
   - Ouvrir le projet dans Android Studio
   - Synchroniser Gradle (File > Sync Project with Gradle Files)

5. **Exécuter l'application**
   - Connecter un appareil Android ou lancer un émulateur
   - Cliquer sur Run

## 📁 Structure du projet

```
app/
├── src/main/
│   ├── java/com/example/rideshare1/
│   │   ├── Activities/          # Activités Android
│   │   ├── Adapters/             # Adaptateurs RecyclerView
│   │   ├── Fragments/            # Fragments
│   │   ├── Models/               # Modèles de données
│   │   ├── Repositories/         # Couche d'accès aux données
│   │   ├── Utils/                # Utilitaires
│   │   └── ViewModels/           # ViewModels MVVM
│   ├── res/
│   │   ├── layout/               # Layouts XML
│   │   ├── menu/                 # Menus
│   │   └── values/               # Ressources (strings, colors, etc.)
│   └── AndroidManifest.xml
└── google-services.json          # Configuration Firebase
```

## 🔐 Sécurité Firebase

Assurez-vous de configurer les règles de sécurité Firestore :

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /trips/{tripId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == resource.data.driverId;
    }
    match /reservations/{reservationId} {
      allow read, write: if request.auth != null;
    }
    match /messages/{messageId} {
      allow read, write: if request.auth != null;
    }
    match /reviews/{reviewId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

## 📱 Captures d'écran

L'application comprend les écrans suivants :
- Écran de démarrage (Splash)
- Connexion / Inscription
- Accueil Passager (Recherche de trajets)
- Accueil Conducteur (Mes trajets)
- Détails d'un trajet
- Création de trajet
- Profil utilisateur
- Messagerie
- Carte Google Maps

## 🛠️ Technologies utilisées

- **Android SDK**: API 24+
- **Firebase Authentication**: Authentification utilisateur
- **Cloud Firestore**: Base de données NoSQL
- **Firebase Storage**: Stockage de fichiers
- **Firebase Cloud Messaging**: Notifications push
- **Google Maps SDK**: Cartes et géolocalisation
- **Material Design Components**: Interface utilisateur
- **Lifecycle Components**: Gestion du cycle de vie
- **Navigation Component**: Navigation entre écrans

## 👥 Équipe

- Maram Zribit
- Iram Ksila
- Khawla Boutar
- Zeineb Souissi
- Fedi Dridi

**Encadrante**: Ahlem Allagui

## 📄 Licence

Ce projet est développé dans le cadre d'un projet académique à l'Esprit School of Business.

## 📝 Notes

- Assurez-vous d'avoir une connexion Internet active pour utiliser Firebase
- Les permissions de localisation sont nécessaires pour Google Maps
- Le fichier `google-services.json` doit être configuré correctement

## 🔧 Dépannage

**Erreur de compilation Firebase**: Vérifiez que le plugin Google Services est bien configuré dans `build.gradle.kts`

**Erreur Google Maps**: Vérifiez que votre clé API est correcte et que les services sont activés dans Google Cloud Console

**Erreur d'authentification**: Vérifiez que Firebase Authentication est activé dans la console Firebase

