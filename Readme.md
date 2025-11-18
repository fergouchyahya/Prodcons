
# Présentation Générale 

Ce projet explore plusieurs approches de synchronisation pour résoudre
le problème classique **Producteurs--Consommateurs**, où plusieurs
threads producteurs génèrent des messages et plusieurs threads
consommateurs les extraient depuis un tampon partagé.

Les différentes versions (`v1` à `v6`) introduisent progressivement :

-   la synchronisation par moniteurs Java (`synchronized`, `wait`,
    `notify`);

-   l'utilisation de sémaphores (`empty`, `full`, `mutex`);

-   l'utilisation de verrous explicites et de variables de condition;

-   la consommation par lots (`get(k)`);

-   les messages en multi-exemplaires synchrones.

Chaque version illustre un concept fondamental de programmation
concurrente.

# Structure du Projet 

``` {style="code"}
prodcons/
├─ options.xml
│
├─ v1/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v2/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v3/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v4/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v5/
│  ├─ IProdConsBuffer.java   # interface étendue avec get(int k)
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
└─ v6/
   ├─ IProdConsBuffer.java   # interface modifiée pour put(m,n)
   ├─ ProdConsBuffer.java
   ├─ Message.java
   │  ├─ Log.java
   ├─ Producer.java
   ├─ Consumer.java
   └─ TestProdCons.java
```

# Exécution 

Chaque version est autonome. Exemple :

``` {style="code"}
mvn -q clean package
mvn -q exec:java -Dexec.mainClass=prodcons.v1.TestProdCons
```

Pour exécuter une version ultérieure, remplacez simplement `v1` par
`v2`, `v3`, etc.

# Fichier de Configuration {#fichier-de-configuration .unnumbered}

``` {style="code"}
<properties>
  <entry key="nProd">3</entry>
  <entry key="nCons">2</entry>
  <entry key="bufSz">5</entry>
  <entry key="prodTime">100</entry>
  <entry key="consTime">150</entry>
  <entry key="minProd">2</entry>
  <entry key="maxProd">5</entry>

  <!-- v5 -->
  <entry key="k">3</entry>

  <!-- v6 -->
  <entry key="nCopies">3</entry>
</properties>
```

# Résumé Conceptuel par Version 

## v1 --- Moniteurs Java 

Tampon implémenté avec `synchronized`, `wait` et `notifyAll`. Les
producteurs attendent lorsque le tampon est plein, et les consommateurs
attendent lorsqu'il est vide. Pas de fin automatique : les consommateurs
tournent sans fin.

## v2 --- Terminaison Déterministe 

Chaque producteur reçoit un **quota fixe**. Exemple :


   Producteur   Quota
  ------------ -------
       P1         3
       P2         5
       P3         2


Total messages attendus : $3 + 5 + 2 = 10$.

Lorsque les 10 messages sont consommés, le système peut s'arrêter
proprement.

## v3 --- Sémaphores 

Utilisation de trois sémaphores :

-   **empty** : nombre de cases libres;

-   **full** : nombre de messages prêts;

-   **mutex** : exclusion mutuelle.

Cette version maximise le parallélisme.

## v4 --- ReentrantLock et Conditions 

Synchronisation plus fine via :

-   un `ReentrantLock` équitable ;

-   deux conditions : `notFull` et `notEmpty`.

Cette version est plus expressive que les moniteurs traditionnels.

## v5 --- Multi-Consommation 

Extension de l'interface :

``` {style="code"}
Message[] get(int k);
```

Problème : si la production finit avant que k messages soient
disponibles, attente infinie. Solution : introduire un **état de fin**
permettant de rendre un lot partiel ou vide pour signaler la
terminaison.

## v6 --- Multi-Exemplaires Synchrones 

Interface modifiée :

``` {style="code"}
void put(Message m, int n);
```

Un producteur dépose un message en $n$ exemplaires.

-   le producteur ne reprend pas tant que les $n$ exemplaires ne sont
    pas consommés ;

-   chaque consommateur impliqué reste aussi bloqué ;

-   le dernier consommateur débloque tout le monde.

Implémentation via des "slots" contenant : le message, le nombre total
d'exemplaires, le nombre consommé et une condition-barrière locale.

# Lecture des Sorties 

Les logs affichent :

-   les insertions et retraits ;

-   l'état du tampon ;

-   les synchronisations (notamment en v6).

Un moniteur d'affichage peut également montrer périodiquement :

-   `nmsg()` : nombre de slots actifs ;

-   `totmsg()` : total d'exemplaires produits.

# Tests 

Chaque version dispose d'un test dédié :

-   mélange du démarrage des threads ;

-   affichage périodique ;

-   mécanisme de terminaison propre ;

-   vérification de la cohérence finale (buffer vide, compteurs
    corrects).

Les tests v6 vérifient en particulier la synchronisation stricte des
multi-exemplaires.

# TO-DO

-   v7 readme after being okay with the implem
-   sump test results in files

## Proposition de refactor pour la terminaison (v2 -> v5)

Contexte
---------
Les versions v2..v5 utilisent aujourd'hui des mécanismes de terminaison
gérés par les tests (les `TestProdCons` attendent la fin des producteurs
puis interrompent/terminent les consommateurs). Pour v5 en particulier,
la consommation par lots (`get(int k)`) complique la terminaison si la
production s'arrête avant qu'un lot complet ne soit disponible.

Objectif
--------
Déplacer la logique de terminaison hors des tests et l'encapsuler dans
le tampon (ou son interface) afin que :

- les producteurs indiquent lorsqu'ils ont terminé de produire ;
- le tampon notifie proprement les consommateurs lorsque la production
    est terminée et qu'il n'y a plus de messages à traiter ;
- la logique de terminaison soit la même pour v2..v5 (et réutilisable).

Proposition d'API
------------------
Ajouter les éléments suivants à `IProdConsBuffer` :

- `void producerDone()` : un producteur appelle cette méthode une fois
    qu'il a fini d'envoyer ses messages. Le tampon maintient un compteur
    de producteurs restants.
- `void close()` ou `void setProducersCount(int n)` (optionnel) :
    alternative pour initialiser le nombre de producteurs (utile pour
    tests automatisés).
- `boolean isClosed()` : indique que tous les producteurs ont appelé
    `producerDone()`.

Comportement attendu
---------------------
- `get()` :
    - si le tampon contient des messages, retourne le message suivant ;
    - si le tampon est vide mais que des producteurs restent actifs, se
        bloque normalement ;
    - si le tampon est vide et que `isClosed()` == true (tous les
        producteurs ont terminé), retourner `null` pour signaler la fin au
        consommateur.

- `get(int k)` (v5) :
    - si au moins `k` messages disponibles -> renvoyer un tableau de taille
        `k` comme aujourd'hui ;
    - si moins de `k` messages disponibles mais `isClosed()` == true ->
        renvoyer un tableau contenant les messages restants (taille < k)
        afin que les consommateurs puissent terminer proprement ;
    - si moins de `k` messages et des producteurs restent -> se bloquer
        comme aujourd'hui.

Avantages
---------
- déplace la responsabilité de terminaison vers la structure de
    synchronisation (meilleure séparation des responsabilités) ;
- simplifie les tests qui n'auront plus à interrompre explicitement les
    consommateurs ;
- résout proprement le cas v5 (lots partiels en fin de production).

Implémentation possible
-----------------------
1. Étendre `IProdConsBuffer` avec `producerDone()` et `isClosed()`.
2. Modifier les `ProdConsBuffer` (v2..v5) pour :
     - conserver un compteur `producersRemaining` initialisé à 0 ou via
         une méthode `setProducersCount(n)` ;
     - décrémenter ce compteur dans `producerDone()` et signaler les
         conditions `notEmpty`/`notFull` le cas échéant ;
     - dans `get()` / `get(int k)` détecter la condition
         "empty && producersRemaining == 0" et retourner `null` ou un
         tableau partiel.
3. Adapter les `Producer` : appeler `producerDone()` juste avant la
     terminaison du thread producteur (ou dans `finally`).
4. Adapter les `Consumer` : considérer que `get()` peut renvoyer
     `null` (ou `Message[]` de taille 0/partielle) et quitter la boucle
     quand la fin est atteinte.

Notes sur le déploiement
------------------------
- Ce changement modifie légèrement l'interface publique `IProdConsBuffer`.
    Il faudra donc appliquer le même changement à toutes les versions
    v2..v5 pour garder la cohérence.
- Les tests (p.ex. `TestProdCons`) deviennent plus simples : après
    avoir démarré les producteurs, chaque producteur appelle
    `producerDone()` à la fin. Les consommateurs quittent automatiquement
    lorsque le tampon est vide et fermé.


Note sur les modifications récentes
-------------------------------
Dans cette branche, les versions `v2` à `v7` ont été modifiées pour
centraliser la logique de terminaison dans le tampon (`IProdConsBuffer` /
`ProdConsBuffer`). Concrètement :

- les producteurs appellent désormais `producerDone()` (ou le buffer est
    construit avec un `expectedTotal` dans le cas de v5) ;
- le tampon sait quand la production est terminée et permet aux
    consommateurs de détecter la fin (retour de `null` pour `get()` ou
    tableau vide pour `get(k)` en v5) ;
- les tests n'interrompent plus les consommateurs explicitement ; ils
    joignent simplement les producteurs puis les consommateurs et la
    terminaison est propre.

Remarque pour `v7` : les fichiers sources originaux ont été conservés
en commentaires directement dans les fichiers modifiés, juste après la
nouvelle implémentation, afin de garder une trace claire du code
initial..

# Conclusion 

Ce projet propose une montée en puissance progressive :

-   des moniteurs simples,

-   aux sémaphores,

-   aux verrous et conditions,

-   jusqu'aux mécanismes avancés de consommation par lots et
    multi-exemplaires synchrones.


