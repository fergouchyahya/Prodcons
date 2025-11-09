package prodcons.v5;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

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
        int k = Integer.parseInt(props.getProperty("k")); // ← taille de lot pour get(k)

        // Tirage des quotas des producteurs (avant démarrage)
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
            }
        }, "Monitor");
        monitor.setDaemon(true);
        monitor.start();

        // Démarrage aléatoire
        Collections.shuffle(all, new Random());
        all.forEach(Thread::start);

        // Attendre la consommation complète
        while (consumed.get() < TOTAL) {
            Thread.sleep(100);
        }

        // Fin contrôlée : interrompre les consommateurs, puis join
        consumers.forEach(Thread::interrupt);
        for (Thread t : producers)
            t.join();
        for (Thread t : consumers)
            t.join();

        System.out.println("== v5 terminé proprement ==");
    }
}
