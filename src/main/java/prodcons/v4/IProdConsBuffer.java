package prodcons.v4;

/**
 * Buffer partagé entre producteurs et consommateurs.
 * Cette interface représente la vue abstraite du tampon :
 * - des producteurs insèrent des Message via put(Message)
 * - des consommateurs retirent des messages via get()
 * Les méthodes nmsg() et totmsg() servent
 * à l'observation / aux tests (statistiques, logs).
 *
 * Le buffer gère aussi la terminaison des producteurs :
 * - le test appelle setProducersCount(n),
 * - chaque producteur appelle producerDone() à sa fin,
 * - quand tous les producteurs sont terminés, le buffer devient "fermé"
 * et peut signaler cette situation aux consommateurs via get().
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
     * Bloque si le tampon est vide jusqu'à l'arrivée d'un message,
     * sauf si le buffer est définitivement fermé (plus de producteurs)
     * et que plus aucun message ne pourra arriver.
     *
     * Convention v4 :
     * - en fonctionnement normal, renvoie un Message non nul ;
     * - si le buffer est fermé ET vide, renvoie {@code null} pour signaler
     * au consommateur qu'il peut terminer.
     *
     * @return le message retiré, ou {@code null} si le buffer est fermé et vide
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
