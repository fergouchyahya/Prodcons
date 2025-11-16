package prodcons.v7;

/**
 * Buffer borné pour l'envoi de tâches entre producteurs et consommateurs.
 *
 * Les producteurs déposent des messages contenant une tâche à exécuter.
 * Les consommateurs récupèrent les messages dans l'ordre FIFO et exécutent
 * la tâche associée.
 */
public interface IProdConsBuffer {

    /**
     * Dépose un message dans le buffer.
     * Se bloque si le buffer est plein.
     */
    void put(Message m) throws InterruptedException;

    /**
     * Récupère un message dans l'ordre FIFO.
     * Se bloque si le buffer est vide.
     */
    Message get() throws InterruptedException;

    /**
     * Nombre de messages actuellement dans le buffer.
     */
    int nmsg();

    /**
     * Nombre total de messages déposés depuis le début.
     */
    int totmsg();
}
