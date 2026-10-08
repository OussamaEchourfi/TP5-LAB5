package com.example.dao;

import com.example.dto.PageResult;
import com.example.model.Reservation;
import com.example.model.Salle;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalleDao {

    private final EntityManager entityManager;

    public SalleDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Étape 5 : Requête JPQL pour récupérer les salles disponibles sur un créneau donné.
     */
    public List<Salle> findSallesDisponibles(LocalDateTime debutRecherche, LocalDateTime finRecherche) {
        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
                      "SELECT r.salle.id FROM Reservation r WHERE " +
                      "(:debut < r.fin AND :fin > r.debut)" +
                      ")";

        TypedQuery<Salle> query = entityManager.createQuery(jpql, Salle.class);
        query.setParameter("debut", debutRecherche);
        query.setParameter("fin", finRecherche);

        return query.getResultList();
    }

    /**
     * Étape 6 : Recherche multi-critères dynamique avec l'API Criteria (sans pagination).
     */
    public List<Salle> rechercherMultiCriteres(String nomRecherche, Integer capaciteMin,
                                               LocalDateTime debutRecherche, LocalDateTime finRecherche) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(cb, cq, salle, nomRecherche, capaciteMin, debutRecherche, finRecherche);

        if (!predicates.isEmpty()) {
            cq.select(salle).where(cb.and(predicates.toArray(new Predicate[0])));
        } else {
            cq.select(salle);
        }

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    /**
     * Étape 7 : Recherche multi-critères dynamique avec pagination côté serveur.
     * @param pageNumber numéro de page (1-based)
     * @param pageSize nombre d'éléments par page
     */
    public PageResult<Salle> rechercherMultiCriteresPagine(String nomRecherche, Integer capaciteMin,
                                                           LocalDateTime debutRecherche, LocalDateTime finRecherche,
                                                           int pageNumber, int pageSize) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Requête principale pour récupérer les éléments paginés
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salleRoot = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(cb, cq, salleRoot, nomRecherche, capaciteMin, debutRecherche, finRecherche);
        if (!predicates.isEmpty()) {
            cq.select(salleRoot).where(cb.and(predicates.toArray(new Predicate[0])));
        } else {
            cq.select(salleRoot);
        }

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        int firstResult = (Math.max(pageNumber, 1) - 1) * pageSize;
        query.setFirstResult(firstResult);
        query.setMaxResults(pageSize);

        List<Salle> sallesPage = query.getResultList();

        // 2. Requête de comptage (Count Query) avec les mêmes prédicats
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Salle> countRoot = countQuery.from(Salle.class);
        countQuery.select(cb.count(countRoot));

        List<Predicate> countPredicates = buildPredicates(cb, countQuery, countRoot, nomRecherche, capaciteMin, debutRecherche, finRecherche);
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        return new PageResult<>(sallesPage, pageNumber, pageSize, totalElements);
    }

    /**
     * Méthode utilitaire réutilisable pour construire les critères de recherche dynamiques.
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            CommonAbstractCriteria query,
                                            Root<Salle> salle,
                                            String nomRecherche,
                                            Integer capaciteMin,
                                            LocalDateTime debutRecherche,
                                            LocalDateTime finRecherche) {
        List<Predicate> predicates = new ArrayList<>();

        // Critère nom (recherche partielle insensible à la casse)
        if (nomRecherche != null && !nomRecherche.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.get("nom")), "%" + nomRecherche.trim().toLowerCase() + "%"));
        }

        // Critère capacité minimale
        if (capaciteMin != null) {
            predicates.add(cb.ge(salle.get("capacite"), capaciteMin));
        }

        // Critère disponibilité (exclure salles ayant des réservations qui chevauchent le créneau)
        if (debutRecherche != null && finRecherche != null) {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Reservation> reservation = subquery.from(Reservation.class);
            subquery.select(reservation.get("salle").get("id"));

            Predicate chevauchement = cb.and(
                cb.lessThan(reservation.get("debut"), finRecherche),
                cb.greaterThan(reservation.get("fin"), debutRecherche)
            );
            subquery.where(chevauchement);

            predicates.add(cb.not(salle.get("id").in(subquery)));
        }

        return predicates;
    }
}
