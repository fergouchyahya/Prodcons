package prodcons.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static java.util.Collections.shuffle;

/**
 * Application de test pour la version v1 du problème producteur-consommateur.
 *
 * Nouvelle logique de terminaison :
 * - on démarre nProd producteurs et nCons consommateurs ;
 * - on attend que TOUS les producteurs aient terminé (join) ;
 * - on attend ensuite que le buffer soit complètement vidé (nmsg() == 0) ;
 * - puis on interrompt proprement tous les consommateurs ;
 * - enfin, on affiche un résumé des statistiques.
 *
 * Aucun arrêt par timer : le test se termine uniquement quand tout le travail
 * a été fait (tous les messages produits ont été consommés).
 */
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

        Log.info("===============================================");
        Log.info("[TEST v1] Démarrage ProdCons (fin quand tout est consommé)");
        Log.info("  nProd   = %d", nProd);
        Log.info("  nCons   = %d", nCons);
        Log.info("  bufSz   = %d", bufSz);
        Log.info("  prodT   = %d ms", prodT);
        Log.info("  consT   = %d ms", consT);
        Log.info("  minProd = %d", minProd);
        Log.info("  maxProd = %d", maxProd);
        Log.info("===============================================");

        List<Thread> all = new ArrayList<>();
        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();

        // Création des producteurs
        for (int i = 0; i < nProd; i++) {
            Thread prod = new Producer(buffer, minProd, maxProd, prodT);
            prod.setName("P-" + i);
            producers.add(prod);
            all.add(prod);
        }

        // Création des consommateurs
        for (int i = 0; i < nCons; i++) {
            Thread cons = new Consumer(buffer, consT);
            cons.setName("C-" + i);
            consumers.add(cons);
            all.add(cons);
        }

        // Démarrage dans un ordre mélangé
        shuffle(all, new Random());
        for (Thread t : all) {
            t.start();
        }

        // 1) Attendre la fin de TOUS les producteurs
        Log.info("[TEST v1] Attente de la fin de tous les producteurs...");
        for (Thread pth : producers) {
            pth.join();
        }
        Log.info("[TEST v1] Tous les producteurs ont terminé. totmsg = %d", buffer.totmsg());

        // 2) Attendre que le buffer soit totalement vidé
        Log.info("[TEST v1] Attente de la vidange complète du buffer...");
        while (buffer.nmsg() > 0) {
            Log.info("[TEST v1] Buffer encore non vide (nmsg=%d, tot=%d)...",
                    buffer.nmsg(), buffer.totmsg());
            try {
                Thread.sleep(50); // petite pause pour éviter un busy-wait agressif
            } catch (InterruptedException e) {
                // pas prévu dans ce test : on ignore et on continue à attendre
            }
        }
        Log.info("[TEST v1] Buffer vide. Interruption des consommateurs...");

        // 3) Interrompre proprement tous les consommateurs
        for (Thread cth : consumers) {
            cth.interrupt();
        }
        for (Thread cth : consumers) {
            cth.join();
        }

        // Résumé final
        Log.info("===============================================");
        Log.info("[TEST v1] Résumé final :");
        Log.info("  totmsg = %d (messages produits au total)", buffer.totmsg());
        Log.info("  nmsg   = %d (messages encore présents dans le buffer)", buffer.nmsg());
        Log.info("===============================================");
    }
}
