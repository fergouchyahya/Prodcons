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



# Conclusion 

Ce projet propose une montée en puissance progressive :

-   des moniteurs simples,

-   aux sémaphores,

-   aux verrous et conditions,

-   jusqu'aux mécanismes avancés de consommation par lots et
    multi-exemplaires synchrones.


