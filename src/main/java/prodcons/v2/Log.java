package prodcons.v2;

import java.util.Date;

/**
 * Utilitaire de journalisation très simple pour le projet ProdCons.
 * - Permet d'activer/désactiver tous les logs via ENABLED.
 * - Synchronise les écritures sur Systemout pour éviter les lignes entrelacées
 * entre plusieurs threads.
 */
public final class Log {

    /**
     * Si false, aucun log n'est produit.
     * Utile pour accélérer les tests ou éviter de polluer la sortie.
     */
    public static volatile boolean ENABLED = true; // coupe/allume les logs

    /**
     * Log formaté.
     * Ajoute une horodatation HH:MM:SS devant le message.
     *
     * @param fmt
     * @param args arguments associés
     */
    public static void info(String fmt, Object... args) {
        if (!ENABLED)
            return;
        // Synchronisation sur System.out pour que les lignes restent entières
        synchronized (System.out) {
            System.out.printf("[%tT] %s%n", new Date(), String.format(fmt, args));
        }
    }

    // Classe utilitaire : constructeur privé pour empêcher l'instanciation.
    private Log() {
    }
}
