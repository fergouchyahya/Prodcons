package prodcons.v6;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test de la version v6 du problème producteur-consommateur.
 *
 * Version v6 = multi-exemplaires synchrone :
 * - un producteur peut déposer un message en n exemplaires via put(m, n),
 * - le producteur reste bloqué tant que tous les exemplaires n'ont pas été
 * consommés,
 * - chaque consommateur qui consomme un exemplaire reste lui aussi bloqué
 * jusqu'à ce que tous les exemplaires de ce message aient été consommés.
 *
 * Ce test :
 * - lit la configuration dans prodcons/options.xml,
 * - tire un quota de messages pour chaque producteur,
 * - connaît le nombre total d'exemplaires attendus,
 * - lance nProd producteurs et nCons consommateurs,
 * - attend la fin de tous les producteurs,
 * - vérifie que le buffer est vide et que le nombre total d'exemplaires
 * produits correspond à ce qui était attendu.
 */
public class TestProdCons {

    public static void main(String[] args) throws Exception {
        // Chargement de la configuration
        Properties props = new Properties();
        try (InputStream in = TestProdCons.class.getResourceAsStream("/prodcons/options.xml")) {
            if (in == null)
                throw new IllegalStateException("prodcons/options.xml introuvable sur le classpath");
            props.loadFromXML(in);
        }

        int nProd = Integer.parseInt(props.getProperty("nProd"));
        int nCons = Integer.parseInt(props.getProperty("nCons"));
        int bufSz = Integer.parseInt(props.getProperty("bufSz"));
        int prodT = Integer.parseInt(props.getProperty("prodTime"));
        int consT = Integer.parseInt(props.getProperty("consTime"));
        int minProd = Integer.parseInt(props.getProperty("minProd"));
        int maxProd = Integer.parseInt(props.getProperty("maxProd"));
        int nCopies = Integer.parseInt(props.getProperty("nCopies")); // nb d'exemplaires par message

        // Tirage des quotas (nombre de messages logiques) pour chaque producteur
        int[] quotas = new int[nProd];
        int totalMessages = 0;
        for (int i = 0; i < nProd; i++) {
            int q = ThreadLocalRandom.current().nextInt(minProd, maxProd + 1);
            quotas[i] = q;
            totalMessages += q;
        }

        // Nombre total d'exemplaires attendus = somme(quota_i) * nCopies
        final int TOTAL_COPIES = totalMessages * nCopies;

        IProdConsBuffer buffer = new ProdConsBuffer(bufSz);
        buffer.setProducersCount(nProd);

        // Affichage de la configuration et des quotas
        System.out.println("==================================================");
        System.out.println("[TEST v6] Démarrage ProdCons v6 (multi-exemplaires synchrone)");
        System.out.printf("  nProd    = %d%n", nProd);
        System.out.printf("  nCons    = %d%n", nCons);
        System.out.printf("  bufSz    = %d%n", bufSz);
        System.out.printf("  prodT    = %d ms%n", prodT);
        System.out.printf("  consT    = %d ms%n", consT);
        System.out.printf("  minProd  = %d%n", minProd);
        System.out.printf("  maxProd  = %d%n", maxProd);
        System.out.printf("  nCopies  = %d (exemplaires par message)%n", nCopies);
        System.out.printf("  total messages logiques attendus = %d%n", totalMessages);
        System.out.printf("  TOTAL exemplaires attendus        = %d%n", TOTAL_COPIES);
        System.out.println("  Quotas par producteur :");
        for (int i = 0; i < nProd; i++) {
            System.out.printf("    P-%d : %d messages%n", i + 1, quotas[i]);
        }
        System.out.println("==================================================");

        List<Thread> all = new ArrayList<>();
        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();

        // Création des producteurs v6 (multi-exemplaires)
        for (int i = 0; i < nProd; i++) {
            Thread t = new Producer(i + 1, buffer, quotas[i], nCopies, prodT);
            producers.add(t);
            all.add(t);
        }

        // Création des consommateurs v6
        for (int i = 0; i < nCons; i++) {
            Thread t = new Consumer(i + 1, buffer, consT);
            consumers.add(t);
            all.add(t);
        }

        // Monitor optionnel pour observer l'état du buffer
        Thread monitor = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(500);
                    System.out.printf("[STAT v6] slots=%d, totCopiesProduced=%d%n",
                            buffer.nmsg(), buffer.totmsg());
                }
            } catch (InterruptedException ignored) {
            }
        }, "Monitor");
        monitor.setDaemon(true);
        monitor.start();

        // Démarrage aléatoire de tous les threads
        Collections.shuffle(all, new Random());
        for (Thread t : all) {
            t.start();
        }

        // Attendre la fin de tous les producteurs.
        // Rappel : un producteur ne quitte pas la méthode put(m, nCopies)
        // tant que tous les exemplaires du message n'ont pas été consommés.
        // Donc, lorsque tous les producteurs ont terminé :
        // - tous les messages (et tous leurs exemplaires) ont été consommés,
        // - le buffer ne devrait plus contenir aucun slot.
        for (Thread pth : producers) {
            pth.join();
        }

        // On laisse un léger délai pour que les derniers consommateurs
        // terminent leur travail.
        Thread.sleep(500);

        int finalSlots = buffer.nmsg();
        int totalProduced = buffer.totmsg();

        // Les consommateurs doivent s'arrêter automatiquement quand le
        // buffer est fermé et vide : on les rejoint donc directement.
        for (Thread c : consumers) {
            c.join();
        }

        // Résumé final et vérification simple
        System.out.println("==================================================");
        System.out.println("[TEST v6] Résumé final :");
        System.out.printf("  TOTAL copies attendues       = %d%n", TOTAL_COPIES);
        System.out.printf("  totalProduced (buffer)       = %d%n", totalProduced);
        System.out.printf("  slots restants dans le buffer = %d%n", finalSlots);

        boolean okProduced = (totalProduced == TOTAL_COPIES);
        boolean okEmpty = (finalSlots == 0);

        System.out.printf("  totalProduced correct        = %s%n", okProduced ? "OUI" : "NON");
        System.out.printf("  buffer entièrement vidé       = %s%n", okEmpty ? "OUI" : "NON");
        System.out.printf("  Test global                  = %s%n",
                (okProduced && okEmpty) ? "SUCCÈS" : "ÉCHEC");
        System.out.println("== v6 terminé proprement ==");
        System.out.println("==================================================");
    }
}
