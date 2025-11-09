package prodcons.v2;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class TestProdCons {
    public static void main(String[] args) throws Exception {
        // Charger la conf
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

        // Tirer les quotas des producteurs AVANT de démarrer
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

        // Créer threads
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

        // (Optionnel) petit monitor
        Thread monitor = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(500);
                    System.out.printf("[STAT] nmsg=%d tot=%d consumed=%d / %d%n",
                            buffer.nmsg(), buffer.totmsg(), consumed.get(), TOTAL);
                }
            } catch (InterruptedException ignored) {
            }
        }, "Monitor");
        monitor.setDaemon(true);
        monitor.start();

        // Démarrage mélangé
        Collections.shuffle(all, new Random());
        for (Thread t : all)
            t.start();

        // Watcher: attendre la consommation complète
        while (consumed.get() < TOTAL) {
            Thread.sleep(100); // polling léger
        }

        // Tous les messages ont été consommés → interruption des consommateurs
        for (Thread c : consumers)
            c.interrupt();

        // Joindre les producteurs (finissent naturellement après leur quota)
        for (Thread pth : producers)
            pth.join();

        // Joindre les consommateurs (interrompus)
        for (Thread c : consumers)
            c.join();

        System.out.println("== v2 terminé proprement ==");
    }
}
