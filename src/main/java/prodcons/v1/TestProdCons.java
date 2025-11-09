package prodcons.v1;

import java.util.*;
import static java.util.Collections.shuffle;

public class TestProdCons {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (var in = TestProdCons.class.getResourceAsStream("/prodcons/options.xml")) {
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

        IProdConsBuffer buffer = new ProdConsBuffer(bufSz);

        List<Thread> all = new ArrayList<>();
        for (int i = 0; i < nProd; i++)
            all.add(new Producer(buffer, minProd, maxProd, prodT));
        for (int i = 0; i < nCons; i++)
            all.add(new Consumer(buffer, consT));

        shuffle(all, new Random()); // démarrage mélangé
        for (Thread t : all)
            t.start();

    }
}
