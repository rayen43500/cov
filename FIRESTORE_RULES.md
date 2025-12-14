# 🔐 Règles de sécurité Firestore - Configuration

## ⚠️ Problème actuel

Vous rencontrez l'erreur : **"PERMISSION_DENIED: Missing or insufficient permissions"**

Cela signifie que les règles de sécurité Firestore ne permettent pas à l'application de lire les données utilisateur après la connexion.

## ✅ Solution : Configurer les règles Firestore

### Étape 1 : Accéder à Firebase Console

1. Aller sur [Firebase Console](https://console.firebase.google.com)
2. Sélectionner votre projet : **rideshare1-dc90e**
3. Dans le menu de gauche, cliquer sur **Firestore Database**
4. Cliquer sur l'onglet **Règles** (Rules)

### Étape 2 : Copier les règles suivantes

Copiez et collez ces règles dans l'éditeur de règles Firestore :

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ========== RÈGLES POUR LES UTILISATEURS ==========
    match /users/{userId} {
      // Lecture : Tous les utilisateurs authentifiés peuvent lire les profils
      allow read: if request.auth != null;
      
      // Écriture : Seul le propriétaire peut modifier son propre profil
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========== RÈGLES POUR LES TRAJETS ==========
    match /trips/{tripId} {
      // Lecture : Tous les utilisateurs authentifiés peuvent lire les trajets
      allow read: if request.auth != null;
      
      // Création : Tous les utilisateurs authentifiés peuvent créer un trajet
      allow create: if request.auth != null 
                    && request.auth.uid == resource.data.driverId;
      
      // Mise à jour : Seul le conducteur propriétaire peut modifier son trajet
      allow update: if request.auth != null 
                    && request.auth.uid == resource.data.driverId;
      
      // Suppression : Seul le conducteur propriétaire peut supprimer son trajet
      allow delete: if request.auth != null 
                    && request.auth.uid == resource.data.driverId;
    }
    
    // ========== RÈGLES POUR LES RÉSERVATIONS ==========
    match /reservations/{reservationId} {
      // Lecture : Le passager ou le conducteur peuvent lire leurs réservations
      allow read: if request.auth != null 
                  && (request.auth.uid == resource.data.passengerId 
                      || request.auth.uid == resource.data.driverId);
      
      // Création : Tous les utilisateurs authentifiés peuvent créer une réservation
      allow create: if request.auth != null;
      
      // Mise à jour : Le passager ou le conducteur peuvent modifier la réservation
      allow update: if request.auth != null 
                    && (request.auth.uid == resource.data.passengerId 
                        || request.auth.uid == resource.data.driverId);
      
      // Suppression : Le passager ou le conducteur peuvent supprimer la réservation
      allow delete: if request.auth != null 
                    && (request.auth.uid == resource.data.passengerId 
                        || request.auth.uid == resource.data.driverId);
    }
    
    // ========== RÈGLES POUR LES MESSAGES ==========
    match /messages/{messageId} {
      // Lecture et écriture : Seulement entre l'expéditeur et le destinataire
      allow read: if request.auth != null 
                  && (request.auth.uid == resource.data.senderId 
                      || request.auth.uid == resource.data.receiverId);
      
      allow create: if request.auth != null;
      
      allow update: if request.auth != null 
                    && (request.auth.uid == resource.data.senderId 
                        || request.auth.uid == resource.data.receiverId);
      
      allow delete: if request.auth != null 
                    && (request.auth.uid == resource.data.senderId 
                        || request.auth.uid == resource.data.receiverId);
    }
    
    // ========== RÈGLES POUR LES AVIS ==========
    match /reviews/{reviewId} {
      // Lecture : Tous les utilisateurs authentifiés peuvent lire les avis
      allow read: if request.auth != null;
      
      // Création : Tous les utilisateurs authentifiés peuvent créer un avis
      allow create: if request.auth != null;
      
      // Mise à jour : Seul l'auteur de l'avis peut le modifier
      allow update: if request.auth != null 
                    && request.auth.uid == resource.data.reviewerId;
      
      // Suppression : Seul l'auteur de l'avis peut le supprimer
      allow delete: if request.auth != null 
                    && request.auth.uid == resource.data.reviewerId;
    }
  }
}
```

### Étape 3 : Publier les règles

1. Cliquer sur le bouton **Publier** (Publish) en haut à droite
2. Attendre la confirmation que les règles ont été publiées
3. Les règles prennent effet immédiatement (quelques secondes)

## 🔍 Vérification

Après avoir publié les règles :

1. **Redémarrer l'application**
2. **Essayer de se connecter** avec `tester@tester.com`
3. **L'erreur devrait disparaître**

## ⚠️ Règles temporaires pour le développement (NON RECOMMANDÉ EN PRODUCTION)

Si vous voulez tester rapidement sans restrictions (⚠️ **UNIQUEMENT POUR LE DÉVELOPPEMENT**) :

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**⚠️ ATTENTION** : Ces règles permettent à tous les utilisateurs authentifiés d'accéder à toutes les données. **Ne pas utiliser en production !**

## 📝 Explication des règles

### Pour les utilisateurs (`users`)
- **Lecture** : Tous les utilisateurs authentifiés peuvent lire les profils (pour afficher les informations des conducteurs, etc.)
- **Écriture** : Seul le propriétaire peut modifier son propre profil

### Pour les trajets (`trips`)
- **Lecture** : Tous peuvent voir les trajets disponibles
- **Création/Modification** : Seul le conducteur propriétaire

### Pour les réservations (`reservations`)
- **Lecture/Modification** : Le passager et le conducteur concernés

### Pour les messages (`messages`)
- **Lecture/Écriture** : Seulement entre l'expéditeur et le destinataire

### Pour les avis (`reviews`)
- **Lecture** : Tous peuvent lire les avis
- **Création** : Tous peuvent créer un avis
- **Modification** : Seul l'auteur

## 🚨 Problèmes courants

### Erreur persiste après avoir publié les règles

1. **Vérifier que vous êtes bien connecté** dans Firebase Console
2. **Attendre 10-30 secondes** pour que les règles se propagent
3. **Redémarrer l'application** complètement
4. **Vérifier que l'authentification Firebase fonctionne** (l'utilisateur est bien connecté)

### Erreur "User document not found"

Cela signifie que le document utilisateur n'existe pas dans Firestore. Solutions :

1. **Créer un compte** via l'application (inscription)
2. **Vérifier dans Firestore Console** que le document existe dans la collection `users`

## ✅ Test rapide

Pour vérifier que les règles fonctionnent :

1. Se connecter avec un compte existant
2. Si la connexion réussit → ✅ Les règles fonctionnent
3. Si l'erreur persiste → Vérifier les étapes ci-dessus

---

**Après avoir configuré ces règles, l'erreur "PERMISSION_DENIED" devrait disparaître !** 🎉

