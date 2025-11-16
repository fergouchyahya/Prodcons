package prodcons.v4;

/**
 * Représente un message échangé entre producteurs et consommateurs.
 *
 * Un message contient :
 * - un identifiant logique (id) généralement unique dans l'exécution,
 * - l'identifiant du thread producteur qui l'a créé (producerTid),
 * ce qui permet de tracer quelle instance de Producer a généré ce message.
 *
 * La méthode toString fournit une représentation compacte adaptée aux logs.
 */
public class Message {

    /**
     * Identifiant logique du message.
     * Typiquement généré par un compteur global côté producteurs.
     */
    public final int id;

    /**
     * Identifiant du thread producteur qui a créé ce message.
     * Permet de savoir d'où vient le message dans les traces.
     */
    public final long producerTid;

    /**
     * Construit un message avec un identifiant et l'id du thread producteur.
     */
    public Message(int id, long producerTid) {
        this.id = id;
        this.producerTid = producerTid;
    }

    /**
     * Représentation texte du message, pensée pour les logs.
     */
    @Override
    public String toString() {
        return "M#" + id + "(P" + producerTid + ")";
    }
}
