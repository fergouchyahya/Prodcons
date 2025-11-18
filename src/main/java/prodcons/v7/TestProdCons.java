package prodcons.v7;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test de la version "tâches" :
 * - les producteurs génèrent des messages contenant des Runnable,
 * - les consommateurs récupèrent les messages et exécutent leur run().
 */
public class TestProdCons {

    public static void main(String[] args) throws Exception {
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

        int[] quotas = new int[nProd];
        int totalMessages = 0;
        for (int i = 0; i < nProd; i++) {
            int q = ThreadLocalRandom.current().nextInt(minProd, maxProd + 1);
            quotas[i] = q;
            totalMessages += q;
        }

        System.out.println("==================================================");
        System.out.println("[TEST v7] ProdCons avec tâches (Message implements Runnable)");
        System.out.printf("  nProd   = %d%n", nProd);
        System.out.printf("  nCons   = %d%n", nCons);
        System.out.printf("  bufSz   = %d%n", bufSz);
        System.out.printf("  prodT   = %d ms%n", prodT);
        System.out.printf("  consT   = %d ms%n", consT);
        System.out.printf("  total messages attendus = %d%n", totalMessages);
        System.out.println("  Quotas par producteur :");
        for (int i = 0; i < nProd; i++) {
            System.out.printf("    P-%d : %d messages%n", i + 1, quotas[i]);
        }
        System.out.println("==================================================");

        IProdConsBuffer buffer = new ProdConsBuffer(bufSz);
        buffer.setProducersCount(nProd);

        List<Thread> all = new ArrayList<>();
        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();

        for (int i = 0; i < nProd; i++) {
            Thread t = new Producer(i + 1, buffer, quotas[i], prodT);
            producers.add(t);
            all.add(t);
        }

        for (int i = 0; i < nCons; i++) {
            Thread t = new Consumer(i + 1, buffer, consT);
            consumers.add(t);
            all.add(t);
        }

        // démarrage aléatoire des threads pour forcer la concurrence
        Collections.shuffle(all, new Random());
        for (Thread t : all) {
            t.start();
        }

        // attendre la fin de tous les producteurs (ils ont un quota connu)
        for (Thread p : producers) {
            p.join();
        }

        // laisser un petit délai aux consommateurs pour vider le tampon
        Thread.sleep(1000);

        // Les consommateurs se terminent automatiquement quand le buffer
        // est fermé et vide : on les rejoint sans les interrompre.
        for (Thread c : consumers) {
            c.join();
        }

        System.out.println("==================================================");
        System.out.println("[TEST v7] Fin du test : toutes les tâches déposées ont été récupérées.");
        System.out.printf("  totalProduced (buffer) = %d%n", buffer.totmsg());
        System.out.printf("  nmsg restant            = %d%n", buffer.nmsg());
        System.out.println("Vérifiez dans la sortie que les tâches sont exécutées dans l'ordre FIFO.");
        System.out.println("==================================================");
    }
}
