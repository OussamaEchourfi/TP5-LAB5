# TP5 : Gestion de Salles avec JPA, JPQL, Criteria API et Pagination::

## 🎯 Objectifs du TP
Ce TP a pour objectif de maîtriser l'implémentation de requêtes complexes avec **JPA** et **Hibernate** dans un contexte d'application Java EE de gestion de salles de réunion/formation :
- Construire des requêtes **JPQL** avancées pour calculer la disponibilité des salles par créneau horaire (gestion des conflits/chevauchements de réservations).
- Mettre en place une recherche multi-critères dynamique avec l'**API Criteria**.
- Intégrer un système de **pagination côté serveur** (`setFirstResult`, `setMaxResults`) et calcul des métadonnées (`count`).
- Structurer le projet selon une architecture multi-couches propre : **Modèle**, **Repository/DAO**, **Service Métier**, **DTO Utilitaire** et **Tests Unitaires JUnit 5**.

---

## 🏗️ Architecture du Projet

```
TP5 LAB5/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   ├── model/
    │   │   │   ├── Salle.java
    │   │   │   └── Reservation.java
    │   │   ├── repository/
    │   │   │   ├── SalleRepository.java
    │   │   │   └── SalleRepositoryImpl.java
    │   │   ├── dao/
    │   │   │   └── SalleDao.java
    │   │   ├── service/
    │   │   │   ├── SalleService.java
    │   │   │   └── SalleServiceImpl.java
    │   │   ├── util/
    │   │   │   └── PaginationResult.java
    │   │   ├── dto/
    │   │   │   └── PageResult.java
    │   │   ├── App.java
    │   │   └── Main.java
    │   └── resources/
    │       └── META-INF/
    │           └── persistence.xml
    └── test/
        └── java/com/example/
            └── SalleServiceTest.java
```

---

## ⚙️ Technologies Utilisées
- **Java** : JDK 21+ / 25
- **ORM / JPA** : Hibernate Core 5.4.30.Final, JPA 2.2 (`javax.persistence`)
- **Base de Données** : H2 Database en mémoire (`jdbc:h2:mem:test`) avec console Web
- **JEE / CDI / EJB** : `javax.ejb-api` 3.2.2, `javax.inject` 1
- **Tests** : JUnit Jupiter 5.10.2
- **Build Tool** : Apache Maven

---

## 🔑 Fonctionnalités Clés Implémentées

### 1. Requête JPQL de Disponibilité par Créneau (Étape 5)
Une salle est considérée comme disponible si elle n'a aucune réservation dont l'intervalle chevauche le créneau demandé :

```java
String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
              "SELECT r.salle.id FROM Reservation r WHERE " +
              "(:debut < r.fin AND :fin > r.debut)" +
              ")";

TypedQuery<Salle> query = entityManager.createQuery(jpql, Salle.class);
query.setParameter("debut", debutRecherche);
query.setParameter("fin", finRecherche);
List<Salle> sallesDisponibles = query.getResultList();
```

---

### 2. Recherche Multi-critères Dynamique avec Criteria API (Étape 6)
Permet de composer dynamiquement des filtres (nom insensible à la casse, capacité minimale, créneau libre) :

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
Root<Salle> salle = cq.from(Salle.class);

List<Predicate> predicates = new ArrayList<>();

// Filtre par nom
if (nom != null && !nom.trim().isEmpty()) {
    predicates.add(cb.like(cb.lower(salle.get("nom")), "%" + nom.trim().toLowerCase() + "%"));
}

// Filtre par capacité
if (capaciteMin != null) {
    predicates.add(cb.ge(salle.get("capacite"), capaciteMin));
}

// Filtre par disponibilité sur créneau
if (debut != null && fin != null) {
    Subquery<Long> subquery = cq.subquery(Long.class);
    Root<Reservation> reservation = subquery.from(Reservation.class);
    subquery.select(reservation.get("salle").get("id"));
    subquery.where(cb.and(
        cb.lessThan(reservation.get("debut"), fin),
        cb.greaterThan(reservation.get("fin"), debut)
    ));
    predicates.add(cb.not(salle.get("id").in(subquery)));
}

cq.select(salle).where(cb.and(predicates.toArray(new Predicate[0])));
```

---

### 3. Pagination Côté Serveur & Métadonnées (Étapes 7 & 10)
Encapsulation des résultats dans `PaginationResult<T>` avec calcul du total des éléments et du nombre de pages :

```java
// Requête paginée
TypedQuery<Salle> query = entityManager.createQuery(cq);
query.setFirstResult((pageNumber - 1) * pageSize);
query.setMaxResults(pageSize);
List<Salle> items = query.getResultList();

// Requête de comptage (Count)
CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
Root<Salle> countRoot = countQuery.from(Salle.class);
countQuery.select(cb.count(countRoot)).where(cb.and(predicates.toArray(new Predicate[0])));
Long totalItems = entityManager.createQuery(countQuery).getSingleResult();

PaginationResult<Salle> result = new PaginationResult<>(items, pageNumber, pageSize, totalItems);
```

---

## 📸 Résultats et Captures d'Écran

### 1. Visualisation des Salles dans la Base H2
Requête : `SELECT * FROM SALLES;`
Toutes les salles initialisées (A, B, C, D, E, F) avec leurs capacités respectives :

![Liste des Salles dans H2](src/main/Screenshot%202026-10-08%20222725.png)

---

### 2. Visualisation des Réservations dans H2
Requête : `SELECT r.ID, s.NOM AS SALLE, r.DEBUT, r.FIN FROM RESERVATIONS r JOIN SALLES s ON r.SALLE_ID = s.ID;`
Visualisation des plages horaires occupées par salle :

![Réservations dans H2](src/main/Screenshot%202026-10-08%20222746.png)

---

### 3. Test de Détection de Disponibilité (SQL / JPQL)
Requête : Salles disponibles entre `2024-06-01 09:00:00` et `2024-06-01 11:00:00` :
Les salles **A** (occupée de 09h à 12h) et **C** (occupée de 10h à 11h) sont correctement exclues. Seules les salles **B, D, E, F** sont retournées :

![Requête Disponibilité H2](src/main/Screenshot%202026-10-08%20222802.png)

---

### 4. Validation des Tests Unitaires JUnit 5
Exécution de la commande `mvn test` validant les 4 cas de tests (Disponibilité, Criteria, Pagination, Règle métier) :

![Résultat Tests JUnit 5](src/main/Screenshot%202026-10-08%20223424.png)

---
