package prodcons.v3;

/**
 * Buffer partagé entre producteurs et consommateurs.
 * Cette interface représente la vue abstraite du tampon :
 * - des producteurs insèrent des Message via put(Message)
 * - des consommateurs retirent des messages via get()
 * Les méthodes nmsg() et totmsg() servent
 * à l'observation / aux tests (statistiques, logs).
 *
 * Dans la version v3, le buffer connaît également :
 * - le nombre de producteurs qui vont produire,
 * - le nombre de consommateurs qui vont consommer,
 * afin de gérer proprement la terminaison (fermeture du buffer).
 */
public interface IProdConsBuffer {

    /**
     * Insère un message dans le buffer.
     * Bloque si le tampon est plein jusqu'à ce qu'une place se libère.
     *
     * @param m message à ajouter
     * @throws InterruptedException si le thread est interrompu pendant l'attente
     */
    void put(Message m) throws InterruptedException;

    /**
     * Retire un message du buffer et le renvoie.
     * Bloque si le tampon est vide jusqu'à l'arrivée d'un message.
     *
     * Dans cette version, lorsque la production est terminée et que le
     * buffer est définitivement vide, l'implémentation peut renvoyer null
     * pour signaler au consommateur qu'il peut terminer.
     *
     * @return le message retiré ou null si le buffer est fermé et vide
     * @throws InterruptedException si le thread est interrompu pendant l'attente
     */
    Message get() throws InterruptedException;

    /**
     * @return nombre de messages actuellement stockés dans le buffer
     */
    int nmsg(); // nombre actuellement dans le buffer

    /**
     * @return nombre total de messages produits depuis le démarrage
     */
    int totmsg(); // nombre total produits depuis le début

    /**
     * Indique au buffer le nombre de producteurs qui vont produire.
     * Doit être appelé une fois avant le démarrage des producteurs (optionnel),
     * ou le test peut s'en occuper.
     * Ce nombre sert à savoir combien de fois la méthode producerDone()
     * devra être appelée avant de considérer que la production est terminée
     * et de fermer le buffer.
     *
     * @param n nombre total de producteurs
     */
    void setProducersCount(int n);

    /**
     * Indique au buffer le nombre de consommateurs qui vont consommer.
     * Doit être appelé une fois avant le démarrage des consommateurs (optionnel),
     * ou le test peut s'en occuper.
     * 
     * Ce nombre permet au buffer de réveiller suffisamment de consommateurs
     * bloqués sur get() lors de la fermeture.
     *
     * @param n nombre total de consommateurs
     */
    void setConsumersCount(int n);

    /**
     * Méthode appelée par un producteur lorsqu'il a terminé sa production.
     * 
     * Après que les n producteurs déclarés via
     * setProducersCount(int)
     * ont tous appelé producerDone(), le buffer peut se marquer comme
     * "fermé" (plus aucune production attendue) et réveiller les consommateurs
     * encore bloqués.
     */
    void producerDone();

    /**
     * Indique si le buffer est fermé, c'est-à-dire si tous les producteurs
     * se sont déclarés terminés via producerDone().
     * <p
     * Un buffer fermé ne reçoit plus de nouveaux messages ; les consommateurs
     * peuvent continuer à vider les messages restants puis s'arrêter lorsque
     * get() renvoie null.
     *
     * true si la production est définitivement terminée
     */
    boolean isClosed();
}
