package prodcons.v5;

/**
 * Interface du tampon ProdCons pour la version v5.
 *
 * Nouveauté par rapport aux versions précédentes :
 * - ajout de get(int k) qui permet de récupérer un lot de k messages
 * consécutifs.
 * k peut être supérieur à la taille du buffer physique, on parle en termes de
 * "flux" logique de messages, pas de taille du tableau.
 */
public interface IProdConsBuffer {

    /**
     * Insère le message m dans le buffer (ordre FIFO).
     */
    void put(Message m) throws InterruptedException;

    /**
     * Récupère un message (FIFO).
     *
     * Peut renvoyer null si la production est terminée et que le tampon est vide.
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
     */
    Message[] get(int k) throws InterruptedException;

    /**
     * Nombre de messages actuellement présents dans le buffer.
     */
    int nmsg();

    /**
     * Nombre total de messages produits depuis le début.
     */
    int totmsg();
}
