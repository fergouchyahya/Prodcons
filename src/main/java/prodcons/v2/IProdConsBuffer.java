package prodcons.v2;

/**
 * Buffer partagé entre producteurs et consommateurs.
 * Cette interface représente la vue abstraite du tampon :
 * - des producteurs insèrent des Message via put(Message)
 * - des consommateurs retirent des messages via get()
 * Les méthodes nmsg() et totmsg() servent
 * à l'observation / aux tests (statistiques, logs).
 */
public interface IProdConsBuffer {

    /**
     * Insère un message dans le buffer.
     * Bloque si le tampon est plein jusqu'à ce qu'une place se libère.
     *
     * @param m message à ajouter
     * @throws InterruptedException si le thread est interrompu pendant l'attente
     */

    /**
     * Indique au buffer le nombre de producteurs qui vont produire.
     * Doit être appelé une fois avant le démarrage des producteurs (optionnel),
     * ou le test peut s'en occuper.
     */
    void setProducersCount(int n);

    /**
     * Signale que ce producteur a terminé sa production.
     * Lorsque tous les producteurs ont appelé producerDone(), le buffer
     * passe en état fermé et les consommateurs en attente doivent pouvoir
     * détecter la fin (get() retourne null).
     */
    void producerDone();

    /**
     * Indique si tous les producteurs ont terminé (buffer fermé).
     */
    boolean isClosed();

    void put(Message m) throws InterruptedException;

    /*
     * Ancienne version de l'interface (conservée en commentaire)
     * package prodcons.v2;
     * 
     * /**
     * Buffer partagé entre producteurs et consommateurs.
     * Cette interface représente la vue abstraite du tampon :
     * - des producteurs insèrent des Message via put(Message)
     * - des consommateurs retirent des messages via get()
     * Les méthodes nmsg() et totmsg() servent
     * à l'observation / aux tests (statistiques, logs).
     */
    /*
     * public interface IProdConsBuffer {
     * 
     * void put(Message m) throws InterruptedException;
     * 
     * Message get() throws InterruptedException;
     * 
     * int nmsg(); // nombre actuellement dans le buffer
     * 
     * int totmsg(); // nombre total produits depuis le début
     * 
     * }
     */

    /**
     * Retire un message du buffer et le renvoie.
     * Bloque si le tampon est vide jusqu'à l'arrivée d'un message.
     *
     * @return le message retiré
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

}
