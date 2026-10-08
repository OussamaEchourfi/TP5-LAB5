package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("SallePU");
        EntityManager em = emf.createEntityManager();

        try {
            // 1. Initialisation des données de test
            em.getTransaction().begin();

            Salle salleA = new Salle("Salle A - Informatique", 30);
            Salle salleB = new Salle("Salle B - Conférence", 100);
            Salle salleC = new Salle("Salle C - Réunion", 15);
            Salle salleD = new Salle("Salle D - Formation", 25);
            Salle salleE = new Salle("Salle E - TP Reseau", 40);
            Salle salleF = new Salle("Salle F - Amphitheatre", 200);

            em.persist(salleA);
            em.persist(salleB);
            em.persist(salleC);
            em.persist(salleD);
            em.persist(salleE);
            em.persist(salleF);

            // Réservations pour tests
            // Salle A réservée le 2026-10-10 de 09:00 à 12:00
            Reservation r1 = new Reservation(salleA,
                    LocalDateTime.of(2026, 10, 10, 9, 0),
                    LocalDateTime.of(2026, 10, 10, 12, 0));

            // Salle B réservée le 2026-10-10 de 14:00 à 18:00
            Reservation r2 = new Reservation(salleB,
                    LocalDateTime.of(2026, 10, 10, 14, 0),
                    LocalDateTime.of(2026, 10, 10, 18, 0));

            // Salle C réservée le 2026-10-10 de 10:00 à 11:30
            Reservation r3 = new Reservation(salleC,
                    LocalDateTime.of(2026, 10, 10, 10, 0),
                    LocalDateTime.of(2026, 10, 10, 11, 30));

            em.persist(r1);
            em.persist(r2);
            em.persist(r3);

            em.getTransaction().commit();

            // 2. Instanciation Repository & Service
            SalleRepository repository = new SalleRepositoryImpl(em);
            SalleService service = new SalleServiceImpl(repository);

            System.out.println("=================================================================");
            System.out.println("=== Étape 8 & 9 : Test de SalleService & SalleRepository       ===");
            System.out.println("=================================================================");

            LocalDateTime debut = LocalDateTime.of(2026, 10, 10, 10, 30);
            LocalDateTime fin = LocalDateTime.of(2026, 10, 10, 13, 0);

            // Test Service getAvailableRooms
            System.out.println("\n[Service] Salles disponibles entre " + debut + " et " + fin + " :");
            List<Salle> disponibles = service.getAvailableRooms(debut, fin);
            for (Salle s : disponibles) {
                System.out.println(" -> " + s.getNom() + " (Capacité : " + s.getCapacite() + ")");
            }

            // Test Service searchRooms avec pagination
            System.out.println("\n[Service] Recherche multi-critères paginée (Nom='Salle', Capacité >= 20, page=1, size=2) :");
            List<Salle> page1 = service.searchRooms("Salle", 20, null, null, 1, 2);
            long total = service.countRooms("Salle", 20, null, null);
            int totalPages = (int) Math.ceil((double) total / 2);

            System.out.println(" Total éléments trouvés: " + total + " (" + totalPages + " pages au total)");
            for (Salle s : page1) {
                System.out.println("  [Page 1] " + s);
            }

            System.out.println("\n[Service] Page 2 :");
            List<Salle> page2 = service.searchRooms("Salle", 20, null, null, 2, 2);
            for (Salle s : page2) {
                System.out.println("  [Page 2] " + s);
            }

        } finally {
            em.close();
            emf.close();
        }
    }
}
