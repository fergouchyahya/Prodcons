package prodcons.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static java.util.Collections.shuffle;

/**
 * Application de test pour la version v1 du problème producteur-consommateur.
 *
 * Rôle :
 * Lire la configuration depuis le fichier prodcons/options.xml.
 * Créer le buffer, les producteurs et les consommateurs.
 * Démarrer les threads dans un ordre mélangé pour favoriser la
 * concurrence.
 * Laisser tourner le système pendant une durée donnée, puis interrompre
 * tous les threads.
 * Afficher un résumé des statistiques finales.
 * 
 *
 * 
 * Remarque : la terminaison est contrôlée par ce test via interruption des
 * threads,
 * car les producteurs/consommateurs de v1 ne se terminent pas d'eux-mêmes.
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

        // Durée du test en millisecondes (optionnelle, défaut : 10 secondes)
        int runTimeMs = Integer.parseInt(p.getProperty("runTimeMs", "10000"));

        IProdConsBuffer buffer = new ProdConsBuffer(bufSz);

        // Affichage de la configuration de test
        Log.info("===============================================");
        Log.info("[TEST v1] Démarrage ProdCons");
        Log.info("  nProd   = %d", nProd);
        Log.info("  nCons   = %d", nCons);
        Log.info("  bufSz   = %d", bufSz);
        Log.info("  prodT   = %d ms", prodT);
        Log.info("  consT   = %d ms", consT);
        Log.info("  minProd = %d", minProd);
        Log.info("  maxProd = %d", maxProd);
        Log.info("  runTime = %d ms", runTimeMs);
        Log.info("===============================================");

        List<Thread> all = new ArrayList<>();

        // Création des producteurs
        for (int i = 0; i < nProd; i++) {
            Thread prod = new Producer(buffer, minProd, maxProd, prodT);
            prod.setName("P-" + i);
            all.add(prod);
        }

        // Création des consommateurs
        for (int i = 0; i < nCons; i++) {
            Thread cons = new Consumer(buffer, consT);
            cons.setName("C-" + i);
            all.add(cons);
        }

        // Démarrage mélangé : l'ordre réel de démarrage est aléatoire
        shuffle(all, new Random());
        for (Thread t : all) {
            t.start();
        }

        // Laisser tourner le système pendant runTimeMs millisecondes
        Thread.sleep(runTimeMs);

        // Demander l'arrêt propre : interruption de tous les threads
        Log.info("[TEST v1] Fin du temps de test, interruption des threads...");
        for (Thread t : all) {
            t.interrupt();
        }

        // Attendre la terminaison de tous les threads
        for (Thread t : all) {
            t.join();
        }

        // Résumé final
        Log.info("===============================================");
        Log.info("[TEST v1] Résumé final :");
        Log.info("  totmsg = %d (messages produits au total)", buffer.totmsg());
        Log.info("  nmsg   = %d (messages encore présents dans le buffer)", buffer.nmsg());
        Log.info("===============================================");
    }
}
