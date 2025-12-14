# 🔧 Solution rapide : Erreur PERMISSION_DENIED

## ⚠️ Problème

Vous voyez l'erreur : **"PERMISSION_DENIED: Missing or insufficient permissions"** lors de la connexion.

## ✅ Solution en 3 étapes

### Étape 1 : Ouvrir Firebase Console

1. Aller sur : https://console.firebase.google.com
2. Sélectionner votre projet : **rideshare1-dc90e**
3. Menu gauche → **Firestore Database**
4. Onglet **Règles** (Rules)

### Étape 2 : Copier ces règles

Remplacez TOUTES les règles existantes par ceci :

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }
    match /trips/{tripId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null && request.auth.uid == resource.data.driverId;
      allow delete: if request.auth != null && request.auth.uid == resource.data.driverId;
    }
    match /reservations/{reservationId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
      allow delete: if request.auth != null;
    }
    match /messages/{messageId} {
      allow read, write: if request.auth != null;
    }
    match /reviews/{reviewId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Étape 3 : Publier

1. Cliquer sur **Publier** (Publish)
2. Attendre 10 secondes
3. Redémarrer l'application
4. Réessayer de se connecter

## ✅ C'est tout !

L'erreur devrait disparaître après ces étapes.

---

**Note** : Pour des règles plus sécurisées en production, consultez `FIRESTORE_RULES.md`

