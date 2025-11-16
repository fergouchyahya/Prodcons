package prodcons.v2;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test de la version v2 du problème producteur-consommateur.
 *
 * Objectif principal : vérifier la terminaison automatique.
 * Tous les producteurs ont un quota fixe de messages, et l'application s'arrête
 * lorsque le nombre total de messages consommés atteint la somme des quotas.
 */
public class TestProdCons {

    public static void main(String[] args) throws Exception {
        // Chargement de la configuration depuis le fichier XML
        var p = new java.util.Properties();
        try (InputStream in = TestProdCons.class.getResourceAsStream("/prodcons/options.xml")) {
            if (in == null)
                throw new IllegalStateException("prodcons/options.xml introuvable sur le classpath");
            p.loadFromXML(in);
        }

        int nProd = Integer.parseInt(p.getProperty("nProd"));
        int nCons = Integer.parseInt(p.getProperty("nCons"));
        int bufSz = Integer.parseInt(p.getProperty("bufSz"));
        int prodT = Integer.parseInt(p.getProperty("prodTime"));
        int consT = Integer.parseInt(p.getProperty("consTime"));
        int minProd = Integer.parseInt(p.getProperty("minProd"));
        int maxProd = Integer.parseInt(p.getProperty("maxProd"));

        // Tirage des quotas des producteurs avant tout démarrage
        int[] quotas = new int[nProd];
        int total = 0;
        for (int i = 0; i < nProd; i++) {
            int q = ThreadLocalRandom.current().nextInt(minProd, maxProd + 1);
            quotas[i] = q;
            total += q;
        }
        final int TOTAL = total;

        IProdConsBuffer buffer = new ProdConsBuffer(bufSz);
        AtomicInteger consumed = new AtomicInteger(0);

        // Affichage de la configuration et des quotas
        System.out.println("===============================================");
        System.out.println("[TEST v2] Démarrage ProdCons v2 (terminaison)");
        System.out.printf("  nProd   = %d%n", nProd);
        System.out.printf("  nCons   = %d%n", nCons);
        System.out.printf("  bufSz   = %d%n", bufSz);
        System.out.printf("  prodT   = %d ms%n", prodT);
        System.out.printf("  consT   = %d ms%n", consT);
        System.out.printf("  minProd = %d%n", minProd);
        System.out.printf("  maxProd = %d%n", maxProd);
        System.out.printf("  TOTAL messages (somme des quotas) = %d%n", TOTAL);
        System.out.println("  Quotas par producteur :");
        for (int i = 0; i < nProd; i++) {
            System.out.printf("    P-%d : %d messages%n", i + 1, quotas[i]);
        }
        System.out.println("===============================================");

        // Création des threads producteurs et consommateurs
        List<Thread> all = new ArrayList<>();
        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();

        for (int i = 0; i < nProd; i++) {
            Thread t = new Producer(i + 1, buffer, quotas[i], prodT);
            producers.add(t);
            all.add(t);
        }
        for (int i = 0; i < nCons; i++) {
            Thread t = new Consumer(i + 1, buffer, consT, consumed);
            consumers.add(t);
            all.add(t);
        }

        // Thread de monitoring périodique
        Thread monitor = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(500);
                    System.out.printf("[STAT v2] nmsg=%d tot=%d consumed=%d / %d%n",
                            buffer.nmsg(), buffer.totmsg(), consumed.get(), TOTAL);
                }
            } catch (InterruptedException ignored) {
            }
        }, "Monitor");
        monitor.setDaemon(true);
        monitor.start();

        // Démarrage mélangé pour bien intercaler producteurs et consommateurs
        Collections.shuffle(all, new Random());
        for (Thread t : all)
            t.start();

        // Boucle de surveillance : on attend que tous les messages soient consommés
        while (consumed.get() < TOTAL) {
            Thread.sleep(100); // polling léger
        }

        // Tous les messages ont été consommés : on interrompt les consommateurs
        System.out.println("[TEST v2] Tous les messages ont été consommés.");
        for (Thread c : consumers)
            c.interrupt();

        // Joindre les producteurs (se terminent naturellement après leur quota)
        for (Thread pth : producers)
            pth.join();

        // Joindre les consommateurs (interrompus)
        for (Thread c : consumers)
            c.join();

        // Résumé final et quelques vérifications simples
        System.out.println("===============================================");
        System.out.println("[TEST v2] Résumé final :");
        System.out.printf("  TOTAL attendu          = %d%n", TOTAL);
        System.out.printf("  totalProduced (buffer) = %d%n", buffer.totmsg());
        System.out.printf("  consumed (compteur)    = %d%n", consumed.get());
        System.out.printf("  nmsg restant dans buf  = %d%n", buffer.nmsg());
        boolean ok = (buffer.totmsg() == TOTAL)
                && (consumed.get() == TOTAL)
                && (buffer.nmsg() == 0);
        System.out.printf("  Terminaison cohérente  = %s%n", ok ? "OUI" : "NON");
        System.out.println("== v2 terminé proprement ==");
        System.out.println("===============================================");
    }
}
