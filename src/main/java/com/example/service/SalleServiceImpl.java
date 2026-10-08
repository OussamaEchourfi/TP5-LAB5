package com.example.service;

import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import com.example.util.PaginationResult;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class SalleServiceImpl implements SalleService {

    @Inject
    private SalleRepository salleRepository;

    public SalleServiceImpl() {
    }

    public SalleServiceImpl(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    public void setSalleRepository(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    /**
     * Permet d'injecter manuellement l'EntityManager lors de tests Java SE standard.
     */
    public void setEntityManager(EntityManager em) {
        this.salleRepository = new SalleRepositoryImpl(em);
    }

    @Override
    public List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        if (debut != null && fin != null && debut.isAfter(fin)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin.");
        }
        return salleRepository.findAvailableRooms(debut, fin);
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        int pageEffective = Math.max(page, 1);
        int sizeEffective = size > 0 ? size : 10;
        return salleRepository.searchRooms(nom, capaciteMin, debut, fin, pageEffective, sizeEffective);
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        return salleRepository.countRooms(nom, capaciteMin, debut, fin);
    }

    @Override
    public PaginationResult<Salle> searchRoomsPaginated(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        int pageEffective = Math.max(page, 1);
        int sizeEffective = size > 0 ? size : 10;

        List<Salle> items = salleRepository.searchRooms(nom, capaciteMin, debut, fin, pageEffective, sizeEffective);
        long totalItems = salleRepository.countRooms(nom, capaciteMin, debut, fin);

        return new PaginationResult<>(items, pageEffective, sizeEffective, totalItems);
    }
}
