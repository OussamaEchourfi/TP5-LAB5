package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;
import com.example.util.PaginationResult;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SalleServiceTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private SalleService salleService;

    @BeforeAll
    void setup() {
        emf = Persistence.createEntityManagerFactory("SallePU");
        em = emf.createEntityManager();

        salleService = new SalleServiceImpl();
        ((SalleServiceImpl) salleService).setEntityManager(em);

        // Insertion des données de test
        em.getTransaction().begin();

        Salle s1 = new Salle("Salle A - Informatique", 30);
        Salle s2 = new Salle("Salle B - Conférence", 100);
        Salle s3 = new Salle("Salle C - Réunion", 15);
        Salle s4 = new Salle("Salle D - Formation", 25);

        em.persist(s1);
        em.persist(s2);
        em.persist(s3);
        em.persist(s4);

        // Réservation sur Salle A : [09:00 - 12:00]
        em.persist(new Reservation(s1,
                LocalDateTime.of(2024, 6, 1, 9, 0),
                LocalDateTime.of(2024, 6, 1, 12, 0)));

        // Réservation sur Salle C : [10:00 - 11:00]
        em.persist(new Reservation(s3,
                LocalDateTime.of(2024, 6, 1, 10, 0),
                LocalDateTime.of(2024, 6, 1, 11, 0)));

        em.getTransaction().commit();
    }

    @AfterAll
    void tearDown() {
        if (em != null) em.close();
        if (emf != null) emf.close();
    }

    @Test
    @DisplayName("Test 1: Récupération des salles disponibles (JPQL)")
    void testFindAvailableRooms() {
        LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 9, 0);
        LocalDateTime fin = LocalDateTime.of(2024, 6, 1, 11, 0);

        List<Salle> disponibles = salleService.getAvailableRooms(debut, fin);

        assertNotNull(disponibles);
        assertEquals(2, disponibles.size(), "Doit trouver exactement 2 salles disponibles (B et D)");
        assertTrue(disponibles.stream().anyMatch(s -> s.getNom().contains("Conférence")));
        assertTrue(disponibles.stream().anyMatch(s -> s.getNom().contains("Formation")));
        assertFalse(disponibles.stream().anyMatch(s -> s.getNom().contains("Informatique")), "Salle A ne doit pas être disponible");
        assertFalse(disponibles.stream().anyMatch(s -> s.getNom().contains("Réunion")), "Salle C ne doit pas être disponible");
    }

    @Test
    @DisplayName("Test 2: Recherche multi-critères dynamique (Criteria API)")
    void testMultiCriteriaSearch() {
        // Recherche des salles avec capacité >= 25 et nom contenant "formation"
        List<Salle> result = salleService.searchRooms("formation", 25, null, null, 1, 10);

        assertEquals(1, result.size());
        assertEquals("Salle D - Formation", result.get(0).getNom());
    }

    @Test
    @DisplayName("Test 3: Pagination et calcul des pages (PaginationResult)")
    void testPagination() {
        // 4 salles au total dans la base, pageSize = 2 -> 2 pages
        PaginationResult<Salle> page1 = salleService.searchRoomsPaginated(null, null, null, null, 1, 2);

        assertEquals(1, page1.getCurrentPage());
        assertEquals(2, page1.getPageSize());
        assertEquals(4, page1.getTotalItems());
        assertEquals(2, page1.getTotalPages());
        assertEquals(2, page1.getItems().size());
    }

    @Test
    @DisplayName("Test 4: Validation de règle métier (date début > date fin)")
    void testInvalidDateRangeThrowsException() {
        LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 12, 0);
        LocalDateTime fin = LocalDateTime.of(2024, 6, 1, 10, 0);

        assertThrows(IllegalArgumentException.class, () -> {
            salleService.getAvailableRooms(debut, fin);
        }, "Une date de début après la date de fin doit lever une IllegalArgumentException");
    }
}
