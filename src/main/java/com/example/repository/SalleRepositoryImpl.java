package com.example.repository;

import com.example.model.Reservation;
import com.example.model.Salle;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

@Stateless
public class SalleRepositoryImpl implements SalleRepository {

    @PersistenceContext(unitName = "SallePU")
    private EntityManager entityManager;

    public SalleRepositoryImpl() {
    }

    public SalleRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
                      "SELECT r.salle.id FROM Reservation r WHERE (:debut < r.fin AND :fin > r.debut))";
        return entityManager.createQuery(jpql, Salle.class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salleRoot = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(cb, cq, salleRoot, nom, capaciteMin, debut, fin);
        if (!predicates.isEmpty()) {
            cq.select(salleRoot).where(cb.and(predicates.toArray(new Predicate[0])));
        } else {
            cq.select(salleRoot);
        }

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        int firstResult = (Math.max(page, 1) - 1) * size;
        query.setFirstResult(firstResult);
        query.setMaxResults(size);

        return query.getResultList();
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Salle> countRoot = countQuery.from(Salle.class);
        countQuery.select(cb.count(countRoot));

        List<Predicate> predicates = buildPredicates(cb, countQuery, countRoot, nom, capaciteMin, debut, fin);
        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            CommonAbstractCriteria query,
                                            Root<Salle> salle,
                                            String nom,
                                            Integer capaciteMin,
                                            LocalDateTime debut,
                                            LocalDateTime fin) {
        List<Predicate> predicates = new ArrayList<>();

        // Critère Nom (recherche insensible à la casse)
        if (nom != null && !nom.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.get("nom")), "%" + nom.trim().toLowerCase() + "%"));
        }

        // Critère Capacité minimale
        if (capaciteMin != null) {
            predicates.add(cb.ge(salle.get("capacite"), capaciteMin));
        }

        // Critère Disponibilité (exclusion des salles ayant un chevauchement)
        if (debut != null && fin != null) {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Reservation> reservation = subquery.from(Reservation.class);
            subquery.select(reservation.get("salle").get("id"));

            Predicate chevauchement = cb.and(
                cb.lessThan(reservation.get("debut"), fin),
                cb.greaterThan(reservation.get("fin"), debut)
            );
            subquery.where(chevauchement);

            predicates.add(cb.not(salle.get("id").in(subquery)));
        }

        return predicates;
    }
}
