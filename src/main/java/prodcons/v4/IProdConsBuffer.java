package prodcons.v4;

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
    void put(Message m) throws InterruptedException;

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
