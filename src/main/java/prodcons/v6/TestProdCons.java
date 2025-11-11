package prodcons.v6;

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
        int nCopies = Integer.parseInt(props.getProperty("nCopies", "3"));

        IProdConsBuffer buffer = new ProdConsBuffer(bufSz);
        AtomicInteger consumed = new AtomicInteger(0);

        // Tirage des quotas (en GROUPES) par producteur, inchangé
        int[] quotas = new int[nProd];
        for (int i = 0; i < nProd; i++) {
            quotas[i] = ThreadLocalRandom.current().nextInt(minProd, maxProd + 1);
        }

        List<Thread> all = new ArrayList<>();
        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();

        // Producteurs (quota = nb de GROUPES), avec nCopies exemplaires par groupe
        for (int i = 0; i < nProd; i++) {
            Thread t = new Producer(i + 1, buffer, quotas[i], nCopies, prodT);
            producers.add(t);
            all.add(t);
        }

        // Consommateurs
        for (int i = 0; i < nCons; i++) {
            Thread t = new Consumer(i + 1, buffer, consT);
            consumers.add(t);
            all.add(t);
        }

        // Monitor optionnel (stats par GROUPES)
        Thread monitor = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(100);
                    System.out.printf("[STAT v6] groups=%d totalGroups=%d%n",
                            buffer.nmsg(), buffer.totmsg());
                }
            } catch (InterruptedException ignored) {
            }
        }, "Monitor");
        monitor.setDaemon(true);
        monitor.start();
        ;

        // Démarrage aléatoire
        Collections.shuffle(all, new Random());
        all.forEach(Thread::start);

        // Attendre que TOUS les producteurs aient fini.
        // En v6, un producteur ne se termine qu'après consommation des n exemplaires de
        // chacun de ses groupes.
        for (Thread t : producers)
            t.join();

        // Laisser respirer pour que les derniers consommateurs de groupe sortent de la
        // barrière
        Thread.sleep(300);

        // Stopper proprement les consommateurs restants (plus de nouveaux groupes)
        for (Thread t : consumers)
            t.interrupt();
        for (Thread t : consumers)
            t.join();

        System.out.println("== v6 terminé proprement ==");
    }
}
