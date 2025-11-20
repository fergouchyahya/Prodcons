package prodcons.v5;

/**
 * Interface du tampon ProdCons pour la version v5.
 *
 * Nouveauté par rapport aux versions précédentes :
 * - ajout de get(int k) qui permet de récupérer un lot de k messages
 * consécutifs.
 *
 * Ici, la terminaison n'est pas gérée par un "producerDone()", mais par le fait
 * que l'implémentation connaît à l'avance le nombre total de messages qui
 * seront produits (expectedTotal dans l'implémentation).
 */
public interface IProdConsBuffer {

    /**
     * Insère le message m dans le buffer (ordre FIFO).
     *
     * Bloque si le buffer est plein jusqu'à ce qu'une place soit libérée
     * par un consommateur.
     *
     * @param m message à insérer
     * @throws InterruptedException si le thread producteur est interrompu
     *                              pendant l'attente
     */
    void put(Message m) throws InterruptedException;

    /**
     * Récupère un message (FIFO).
     *
     * Peut renvoyer null si la production est terminée et que le tampon est vide.
     * (Cette méthode est laissée pour compatibilité, mais dans v5 on utilise
     * surtout get(int) côté consommateurs.)
     *
     * @return un message non nul en cas de succès, ou null si plus aucun
     *         message ne pourra jamais arriver.
     * @throws InterruptedException si le thread consommateur est interrompu
     *                              pendant l'attente
     */
    Message get() throws InterruptedException;

    /**
     * Récupère k messages consécutifs (FIFO).
     *
     * Selon l'état de la production et du tampon, la taille du tableau
     * retourné peut être :
     * - exactement k si suffisamment de messages sont disponibles,
     * - strictement comprise entre 1 et k si la production est terminée mais
     * qu'il reste moins de k messages à vider,
     * - 0 (tableau vide) si la production est terminée et que le tampon est vide.
     *
     * Le tableau retourné ne contient jamais de null.
     *
     * @param k taille cible du lot (doit être strictement positive)
     * @return un tableau de 0 à k messages
     * @throws InterruptedException     si le thread consommateur est interrompu
     *                                  pendant l'attente
     * @throws IllegalArgumentException si k <= 0
     */
    Message[] get(int k) throws InterruptedException;

    /**
     * Nombre de messages actuellement présents dans le buffer.
     * Méthode prévue pour l'observation / les statistiques.
     *
     * @return nombre d'éléments stockés
     */
    int nmsg();

    /**
     * Nombre total de messages produits depuis le début.
     * Ne diminue jamais. En v5, quand cette valeur atteint la valeur
     * attendue (expectedTotal), la production est considérée comme terminée.
     *
     * @return nombre total de messages passés par le buffer
     */
    int totmsg();

    /**
     * (Buffer‑centré) Initialise le nombre de producteurs attendus.
     * Doit être appelé avant le démarrage des producteurs dans les tests
     * qui utilisent cette méthode.
     *
     * @param n nombre de producteurs
     */
    void setProducersCount(int n);

    /**
     * Un producteur appelle cette méthode lorsqu'il a fini sa production
     * (à placer idéalement dans un bloc finally du thread producteur).
     * Le buffer décrémente alors le compteur interne et, si c'était le
     * dernier producteur, signale la fin de la production aux consommateurs.
     */
    void producerDone();

    /**
     * Indique si la production globale est terminée (tous les producteurs
     * ont appelé {@link #producerDone()}).
     *
     * @return true si la production est close
     */
    boolean isClosed();
}
