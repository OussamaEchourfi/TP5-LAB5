package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;
import com.example.util.PaginationResult;
import org.h2.tools.Server;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        Server h2WebServer = null;
        try {
            // Démarrage du serveur Web H2 pour visualiser la base de données dans le navigateur
            h2WebServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            System.out.println("==========================================================================");
            System.out.println("🌐 Console Web H2 active sur : http://localhost:8082");
            System.out.println("   Paramètres de connexion :");
            System.out.println("   - Driver Class : org.h2.Driver");
            System.out.println("   - JDBC URL     : jdbc:h2:mem:test");
            System.out.println("   - User Name    : sa");
            System.out.println("   - Password     : (laisser vide)");
            System.out.println("==========================================================================\n");
        } catch (SQLException e) {
            System.err.println("Impossible de démarrer la console H2 : " + e.getMessage());
        }

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("SallePU");
        EntityManager em = emf.createEntityManager();

        try {
            // ==========================================================
            // 1. Initialisation d'un jeu de données de test complet
            // ==========================================================
            em.getTransaction().begin();

            Salle salleA = new Salle("Salle A - Informatique", 30);
            Salle salleB = new Salle("Salle B - Conférence", 100);
            Salle salleC = new Salle("Salle C - Réunion", 15);
            Salle salleD = new Salle("Salle D - Formation", 25);
            Salle salleE = new Salle("Salle E - TP Réseau", 40);
            Salle salleF = new Salle("Salle F - Amphithéâtre", 250);

            em.persist(salleA);
            em.persist(salleB);
            em.persist(salleC);
            em.persist(salleD);
            em.persist(salleE);
            em.persist(salleF);

            // Réservations :
            // Salle A réservée de 09h00 à 12h00
            Reservation resA = new Reservation(salleA,
                    LocalDateTime.of(2024, 6, 1, 9, 0),
                    LocalDateTime.of(2024, 6, 1, 12, 0));

            // Salle C réservée de 10h00 à 11h00
            Reservation resC = new Reservation(salle3(salleC),
                    LocalDateTime.of(2024, 6, 1, 10, 0),
                    LocalDateTime.of(2024, 6, 1, 11, 0));

            // Salle E réservée l'après-midi de 14h00 à 17h00
            Reservation resE = new Reservation(salleE,
                    LocalDateTime.of(2024, 6, 1, 14, 0),
                    LocalDateTime.of(2024, 6, 1, 17, 0));

            em.persist(resA);
            em.persist(resC);
            em.persist(resE);

            em.getTransaction().commit();

            // Création et injection du service
            SalleService salleService = new SalleServiceImpl();
            ((SalleServiceImpl) salleService).setEntityManager(em);

            System.out.println("==========================================================================");
            System.out.println("🧪 SCÉNARIOS DE TEST DU TP5");
            System.out.println("==========================================================================");

            // ---------------------------------------------------------------------------------
            // TEST 1 : Disponibilité par créneau (Requête JPQL)
            // ---------------------------------------------------------------------------------
            System.out.println("\n[TEST 1] Requête JPQL - Disponibilité sur créneau [2024-06-01 de 09:00 à 11:00]");
            LocalDateTime debut1 = LocalDateTime.of(2024, 6, 1, 9, 0);
            LocalDateTime fin1 = LocalDateTime.of(2024, 6, 1, 11, 0);

            List<Salle> dispo1 = salleService.getAvailableRooms(debut1, fin1);
            System.out.println("Résultat attendu : Salles B, D, E, F (exclut A et C qui ont des réservations)");
            for (Salle s : dispo1) {
                System.out.println("  ✓ " + s.getNom() + " (Capacité : " + s.getCapacite() + ")");
            }

            // ---------------------------------------------------------------------------------
            // TEST 2 : Recherche multi-critères dynamique (Criteria API)
            // ---------------------------------------------------------------------------------
            System.out.println("\n[TEST 2] Recherche Multi-critères dynamique");
            
            // Cas 2.1 : Filtre Nom seul
            System.out.println(" - Cas 2.1 : Nom contenant 'formation'");
            List<Salle> res21 = salleService.searchRooms("formation", null, null, null, 1, 10);
            res21.forEach(s -> System.out.println("    -> " + s));

            // Cas 2.2 : Filtre Capacité seule (>= 40)
            System.out.println(" - Cas 2.2 : Capacité minimale >= 40");
            List<Salle> res22 = salleService.searchRooms(null, 40, null, null, 1, 10);
            res22.forEach(s -> System.out.println("    -> " + s));

            // Cas 2.3 : Filtres combinés (Nom='Salle' + Capacité >= 30 + Disponible entre 14h et 16h)
            LocalDateTime debutAprem = LocalDateTime.of(2024, 6, 1, 14, 0);
            LocalDateTime finAprem = LocalDateTime.of(2024, 6, 1, 16, 0);
            System.out.println(" - Cas 2.3 : Nom='Salle' + Capacité >= 30 + Créneau [14:00 - 16:00]");
            List<Salle> res23 = salleService.searchRooms("Salle", 30, debutAprem, finAprem, 1, 10);
            System.out.println("    Résultat attendu : Salles A, B, F (exclut E qui est occupée de 14h à 17h, C et D car capacité < 30)");
            res23.forEach(s -> System.out.println("    -> " + s));

            // ---------------------------------------------------------------------------------
            // TEST 3 : Pagination et calcul des métadonnées (PaginationResult)
            // ---------------------------------------------------------------------------------
            System.out.println("\n[TEST 3] Pagination côté serveur (Filtre: Capacité >= 20, pageSize = 2)");
            PaginationResult<Salle> page1 = salleService.searchRoomsPaginated(null, 20, null, null, 1, 2);
            System.out.println("Métadonnées : Total éléments = " + page1.getTotalItems() + ", Total pages = " + page1.getTotalPages());
            System.out.println("Page 1 (items: " + page1.getItems().size() + ") :");
            page1.getItems().forEach(s -> System.out.println("  [Page 1] " + s));

            PaginationResult<Salle> page2 = salleService.searchRoomsPaginated(null, 20, null, null, 2, 2);
            System.out.println("Page 2 (items: " + page2.getItems().size() + ") :");
            page2.getItems().forEach(s -> System.out.println("  [Page 2] " + s));

            PaginationResult<Salle> page3 = salleService.searchRoomsPaginated(null, 20, null, null, 3, 2);
            System.out.println("Page 3 (items: " + page3.getItems().size() + ") :");
            page3.getItems().forEach(s -> System.out.println("  [Page 3] " + s));

            // ---------------------------------------------------------------------------------
            // TEST 4 : Règle métier dans le Service (Gestion d'erreur)
            // ---------------------------------------------------------------------------------
            System.out.println("\n[TEST 4] Règle métier Service - Validation date début > date fin");
            try {
                salleService.getAvailableRooms(fin1, debut1); // Début après fin
            } catch (IllegalArgumentException ex) {
                System.out.println("  ✓ Exception levée avec succès : " + ex.getMessage());
            }

            System.out.println("\n==========================================================================");
            System.out.println("💡 Base de données H2 en mémoire active !");
            System.out.println("   Console Web : http://localhost:8082");
            System.out.println("   JDBC URL    : jdbc:h2:mem:test | User : sa | Mot de passe : (vide)");
            System.out.println("==========================================================================");
            System.out.println(">>> Appuyez sur [Entrée] dans la console pour fermer l'application...");

            // Permet de garder le serveur H2 ouvert si exécuté en mode interactif
            if (System.console() != null || System.in.available() >= 0) {
                try {
                    System.in.read();
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
            if (h2WebServer != null) {
                h2WebServer.stop();
                System.out.println("Serveur H2 arrêté.");
            }
        }
    }

    private static Salle salle3(Salle s) {
        return s;
    }
}
