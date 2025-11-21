package prodcons.v6;

/**
 * Interface du tampon ProdCons pour la version v6.
 *
 * Nouveauté : un producteur peut déposer un message en n exemplaires,
 * et cette production est synchronisée avec n consommateurs.
 *
 * La synchronisation v6 se fait au niveau du message :
 * - le producteur de m reste bloqué tant que tous les n exemplaires
 * n'ont pas été consommés,
 * - chaque consommateur qui prend un exemplaire de m reste bloqué tant
 * que tous les exemplaires de m n'ont pas été consommés.
 */
public interface IProdConsBuffer {

    /**
     * Dépose n exemplaires du message m dans le buffer.
     *
     * Le thread producteur est bloqué jusqu'à ce que tous les exemplaires
     * du message aient été consommés par des consommateurs.
     *
     * L'ordre logique des messages reste FIFO au niveau des "messages
     * logiques" (slots), même si chaque message possède plusieurs copies.
     *
     * @param m le message à déposer
     * @param n nombre d'exemplaires du message (strictement positif)
     * @throws InterruptedException si le producteur est interrompu pendant
     *                              l'attente (buffer plein ou attente que
     *                              tous les exemplaires soient consommés)
     */
    void put(Message m, int n) throws InterruptedException;

    /**
     * Récupère un exemplaire de message dans l'ordre FIFO logique.
     *
     * Un consommateur qui obtient un message restera bloqué tant que tous
     * les exemplaires de ce message n'ont pas été consommés, conformément
     * à la spécification de la v6. Lorsque get() rend la main avec un
     * Message non nul, la synchronisation multi-exemplaires est déjà
     * réalisée pour ce message.
     *
     * Convention de terminaison :
     * - renvoie un Message non nul en fonctionnement normal,
     * - renvoie  null si le buffer est fermé (tous les producteurs
     * ont terminé) et qu'aucun message ne reste à consommer.
     *
     * @return un message du buffer, ou  null si le buffer est fermé
     *         et définitivement vide
     * @throws InterruptedException si le consommateur est interrompu pendant
     *                              l'attente (buffer vide, synchronisation)
     */
    Message get() throws InterruptedException;

    /**
     * Nombre de messages actuellement dans le buffer.
     * Ici, ce nombre correspond au nombre de "slots logiques"
     * (messages différents encore présents), pas au nombre d'exemplaires.
     *
     * @return nombre de slots actuellement présents
     */
    int nmsg();

    /**
     * Nombre total d'exemplaires de messages produits depuis le début.
     *
     * Un message déposé avec n copies contribue de n à ce total.
     *
     * @return nombre total d'exemplaires produits
     */
    int totmsg();

    /**
     * Informe le buffer du nombre total de producteurs qui vont produire.
     * Utilisé pour gérer la fermeture propre du buffer :
     * lorsque tous les producteurs ont appelé producerDone(),
     * le buffer est marqué comme fermé.
     *
     * @param n nombre de producteurs
     */
    void setProducersCount(int n);

    /**
     * Méthode appelée par un producteur lorsqu'il a terminé sa production.
     *
     * Quand le nombre d'appels à producerDone() atteint celui passé à
     * setProducersCount(int), le buffer peut :
     * - se marquer comme fermé,
     * - réveiller les consommateurs bloqués sur get() pour qu'ils constatent
     * la fermeture (get() renverra alors null s'il n'y a plus de slot).
     */
    void producerDone();

    /**
     * Indique si le buffer est fermé, c'est-à-dire si tous les producteurs
     * se sont déclarés terminés via producerDone().
     *
     * Un buffer fermé ne reçoit plus de nouveaux slots, mais peut encore
     * contenir des messages en attente de consommation.
     *
     * @return true si la production est définitivement terminée
     */
    boolean isClosed();
}
