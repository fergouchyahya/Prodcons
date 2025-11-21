package prodcons.v5;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test de la version v5 du problème producteur-consommateur.
 *
 * Nouveauté de v5 :
 * - ajout de la multi-consommation via get(k),
 * - terminaison gérée grâce au fait que le tampon connaît le nombre total
 * de messages attendus (expectedTotal).
 *
 * Le scénario :
 * - on tire un quota aléatoire pour chaque producteur,
 * - on calcule TOTAL = somme de ces quotas,
 * - ProdConsBuffer est construit avec expectedTotal = TOTAL,
 * - les consommateurs lisent par lots de taille k,
 * - on compte globalement les messages consommés,
 * - à la fin, on vérifie que consumed == TOTAL, totmsg == TOTAL et nmsg == 0.
 */
public class TestProdCons {

    public static void main(String[] args) throws Exception {
        // Charger la configuration
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
        int k = Integer.parseInt(props.getProperty("k")); // taille de lot pour get(k)

        // Tirage des quotas des producteurs (avant démarrage)
        int[] quotas = new int[nProd];
        int total = 0;
        for (int i = 0; i < nProd; i++) {
            int q = ThreadLocalRandom.current().nextInt(minProd, maxProd + 1);
            quotas[i] = q;
            total += q;
        }
        final int TOTAL = total;

        // Le buffer est construit avec la capacité ; on initialise ensuite
        // le nombre de producteurs attendus pour la logique de terminaison
        // buffer‑centrée.
        ProdConsBuffer buffer = new ProdConsBuffer(bufSz);
        buffer.setProducersCount(nProd);
        AtomicInteger consumed = new AtomicInteger(0);

        // Affichage de la configuration et des quotas
        System.out.println("===============================================");
        System.out.println("[TEST v5] Démarrage ProdCons v5 (multi-consommation)");
        System.out.printf("  nProd   = %d%n", nProd);
        System.out.printf("  nCons   = %d%n", nCons);
        System.out.printf("  bufSz   = %d%n", bufSz);
        System.out.printf("  prodT   = %d ms%n", prodT);
        System.out.printf("  consT   = %d ms%n", consT);
        System.out.printf("  minProd = %d%n", minProd);
        System.out.printf("  maxProd = %d%n", maxProd);
        System.out.printf("  k (taille des lots) = %d%n", k);
        System.out.printf("  TOTAL messages (somme des quotas) = %d%n", TOTAL);
        System.out.println("  Quotas par producteur :");
        for (int i = 0; i < nProd; i++) {
            System.out.printf("    P-%d : %d messages%n", i + 1, quotas[i]);
        }
        System.out.println("===============================================");

        List<Thread> all = new ArrayList<>();
        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();

        // Producteurs (quota fixe)
        for (int i = 0; i < nProd; i++) {
            Thread t = new Producer(i + 1, buffer, quotas[i], prodT);
            producers.add(t);
            all.add(t);
        }

        // Consommateurs (lecture par lots de k)
        for (int i = 0; i < nCons; i++) {
            Thread t = new Consumer(i + 1, buffer, consT, k, consumed);
            consumers.add(t);
            all.add(t);
        }

        // Monitor optionnel (statistiques périodiques)
        Thread monitor = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(500);
                    System.out.printf("[STAT v5] nmsg=%d tot=%d consumed=%d / %d (k=%d)%n",
                            buffer.nmsg(), buffer.totmsg(), consumed.get(), TOTAL, k);
                }
            } catch (InterruptedException ignored) {
                // Interruption normale du monitor à la fin de l'exécution.
            }
        }, "Monitor");
        monitor.setDaemon(true);
        monitor.start();

        // Démarrage aléatoire pour mélanger producteurs et consommateurs
        Collections.shuffle(all, new Random());
        all.forEach(Thread::start);

        // À ce stade :
        // - tous les messages ont été consommés (consumed == TOTAL),
        // - les producteurs finissent naturellement après leur quota,
        // - les consommateurs finissent lorsque get(k) renvoie un lot vide.
        for (Thread t : producers)
            t.join();
        for (Thread t : consumers)
            t.join();

        // Résumé final avec vérification de cohérence
        System.out.println("===============================================");
        System.out.println("[TEST v5] Résumé final :");
        System.out.printf("  TOTAL attendu          = %d%n", TOTAL);
        System.out.printf("  totalProduced (buffer) = %d%n", buffer.totmsg());
        System.out.printf("  consumed (compteur)    = %d%n", consumed.get());
        System.out.printf("  nmsg restant dans buf  = %d%n", buffer.nmsg());
        boolean ok = (buffer.totmsg() == TOTAL)
                && (consumed.get() == TOTAL)
                && (buffer.nmsg() == 0);
        System.out.printf("  Terminaison cohérente  = %s%n", ok ? "OUI" : "NON");
        System.out.println("== v5 terminé proprement (multi-consommation) ==");
        System.out.println("===============================================");
    }
}
