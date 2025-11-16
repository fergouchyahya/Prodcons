package prodcons.v6;

/**
 * Interface du tampon ProdCons pour la version v6.
 *
 * Nouveauté : un producteur peut déposer un message en n exemplaires,
 * et cette production est synchronisée avec n consommateurs.
 */
public interface IProdConsBuffer {

    /**
     * Dépose n exemplaires du message m dans le buffer.
     *
     * Le thread producteur est bloqué jusqu'à ce que tous les exemplaires
     * du message aient été consommés.
     *
     * @param m le message à déposer
     * @param n nombre d'exemplaires du message
     */
    void put(Message m, int n) throws InterruptedException;

    /**
     * Récupère un exemplaire de message dans l'ordre FIFO logique.
     *
     * Un consommateur qui obtient un message restera bloqué tant que tous
     * les exemplaires de ce message n'ont pas été consommés, conformément
     * à la spécification de la v6.
     *
     * @return un message du buffer
     */
    Message get() throws InterruptedException;

    /**
     * Nombre de messages actuellement dans le buffer.
     * Ici, ce nombre correspond au nombre de "slots logiques"
     * (messages différents encore présents), pas au nombre d'exemplaires.
     */
    int nmsg();

    /**
     * Nombre total d'exemplaires de messages produits depuis le début.
     */
    int totmsg();
}
