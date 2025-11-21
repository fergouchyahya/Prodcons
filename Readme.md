# Projet ProdCons : Implémentations v1 à v7 

Ce projet présente plusieurs implémentations progressives du problème
**Producteurs--Consommateurs**. Chaque version introduit des mécanismes
de synchronisation différents : moniteurs Java, sémaphores, verrous
explicites, consommation par lots et synchronisation multi-exemplaires.

L'objectif global est de comparer diverses approches de synchronisation
et d'unifier la *gestion de terminaison* via un modèle
**buffer-centré**.

# Structure du projet

    prodcons/
     ├─ resources/prodcons/options.xml
     ├─ v1/ ... v7/
     │    IProdConsBuffer.java
     │    ProdConsBuffer.java
     │    Producer.java
     │    Consumer.java
     │    Message.java
     │    Log.java
     │    TestProdCons.java
     └─ v7/
          TaskExecutor.java
          TestTaskExecutor.java

Le fichier `options.xml` configure tous les tests : nombre de
producteurs/consommateurs, tailles de tampon, délais, paramètres
spécifiques à v5 (k) ou v6 (nCopies), etc.

# Compilation et exécution

    mvn -q clean package

Exécution d'une version :

    java -cp target/classes prodcons.v3.TestProdCons
    java -cp target/classes prodcons.v5.TestProdCons
    java -cp target/classes prodcons.v7.TestTaskExecutor

La configuration est modifiable dans :
`src/main/resources/prodcons/options.xml`.

# Contrat buffer-centré

Les versions modernes utilisent un modèle uniforme de terminaison :

-   **`setProducersCount(n)`** : nombre de producteurs attendus.

-   **`producerDone()`** : appelé en `finally` par chaque producteur.

-   **`isClosed()`** : vrai lorsque tous les producteurs ont terminé.

-   **`get()`** :

    -   bloque si le tampon est vide et la production active ;

    -   renvoie `null` si le tampon est vide et fermé.

-   **`get(k)`** (v5) :

    -   renvoie *k* messages si disponibles ;

    -   renvoie un lot partiel si `isClosed()`;

    -   renvoie un tableau vide si plus rien n'est disponible.

Ce contrat garantit une terminaison propre des consommateurs, sans
intervention externe.

# Versions et mécanismes

## v1 --- Moniteurs Java 

-   utilisation de `synchronized`, `wait()`, `notifyAll()`;

-   implémentation minimale du tampon circulaire.

## v2 --- Moniteurs + quotas 

-   chaque producteur possède un quota ;

-   la somme des quotas permet une terminaison déterministe.

## v3 --- Sémaphores 

-   `empty`, `full`, `mutex`;

-   reproduction de l'algorithme classique Producteur--Consommateur.

## v4 --- Locks et Conditions 

-   synchronisation via `ReentrantLock` équitable ;

-   conditions `notFull` / `notEmpty`.

## v5 --- Consommation par lots 

-   méthode `get(k)` pour récupérer un lot de messages ;

-   gestion automatique des cas partiels en fin de production.

## v6 --- Multi-exemplaires synchrones 

-   dépôt d'un message en *n* exemplaires ;

-   producteur + consommateurs synchronisés via une barrière locale ;

-   suppression du slot lorsque tous les exemplaires sont consommés.

## v7 --- Tâches et exécuteur dynamique 

-   les messages encapsulent un `Runnable` exécuté par les consommateurs
    ;

-   **TaskExecutor** : mini pool de threads dynamique :

    -   création automatique de workers ;

    -   expiration après 3s d'inactivité.

# Observabilité

Les tests affichent périodiquement :

-   **nmsg** : nombre d'éléments dans le tampon ;

-   **totmsg** : total produit ;

-   **consumed** (v3--v5) : total consommé ;

-   **slots** (v6) : nombre de slots actifs.

La terminaison est correcte lorsque :

-   `isClosed()` est vrai ;

-   le tampon est vide ;

-   les compteurs sont cohérents.

# Conclusion 

Ce projet illustre la progression naturelle des mécanismes de
synchronisation en Java, depuis les moniteurs jusqu'aux scénarios
avancés comme la consommation par lots, les messages multi-exemplaires
et l'exécution de tâches. Le modèle **buffer-centré** fournit une
logique de terminaison robuste, cohérente et facile à tester.
