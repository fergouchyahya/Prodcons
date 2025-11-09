
**Projet Producteurs--Consommateurs**\
Polytech Grenoble --- Module PC-SE\
Yahya Fergouch --- Novembre 2025


------------------------------------------------------------------------

# Présentation Générale 

L'objectif est de comprendre les mécanismes de :

-   Synchronisation entre threads producteurs et consommateurs ;

-   Communication via un tampon (buffer) partagé ;

-   Gestion correcte de la terminaison du système ;

-   Utilisation de verrous, moniteurs et sémaphores.

Chaque version (`v1` à `v6`) explore une méthode différente pour
résoudre le même problème.

------------------------------------------------------------------------

# Structure du Projet 

``` {.text fontsize="\\small"}
prodcons/
├─ options.xml
├─ v1/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v2/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v3/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v4/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
├─ v5/
│  ├─ IProdConsBuffer.java   # interface étendue avec get(int k)
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
│
└─ v6/
   ├─ IProdConsBuffer.java   # interface modifiée avec put(Message,int)
   ├─ ProdConsBuffer.java
   ├─ Message.java
   ├─ Producer.java
   ├─ Consumer.java
   └─ TestProdCons.java
```

------------------------------------------------------------------------

# Exécution du Projet 

Chaque version est autonome et peut être exécutée via Maven :

``` {.bash fontsize="\\small"}
mvn -q clean package
mvn -q exec:java -Dexec.mainClass=prodcons.v1.TestProdCons
```

Pour exécuter une autre version, remplacez simplement `v1` par `v2`,
`v3`, etc.

# Fichier de Configuration 

Le fichier `options.xml` définit les paramètres du système :

``` {.xml fontsize="\\small"}
<properties>
  <entry key="nProd">3</entry>       <!-- nombre de producteurs -->
  <entry key="nCons">2</entry>       <!-- nombre de consommateurs -->
  <entry key="bufSz">5</entry>       <!-- taille du tampon -->
  <entry key="prodTime">100</entry>  <!-- délai de production (ms) -->
  <entry key="consTime">150</entry>  <!-- délai de consommation (ms) -->
  <entry key="minProd">2</entry>     <!-- min messages par producteur -->
  <entry key="maxProd">5</entry>     <!-- max messages par producteur -->
</properties>
```

------------------------------------------------------------------------

# Principe du Fonctionnement --- Version 2 

Chaque producteur reçoit un **quota fixe** de messages à produire.

::: center
   **Producteur**   **Messages à produire**  
  ---------------- ------------------------- --
         1                     3             
         2                     5             
         3                     2             
:::

Le total est donc : $$TOTAL = 3 + 5 + 2 = 10~\text{messages.}$$

Cette valeur est cruciale : elle indique combien de messages existeront
dans tout le système. Dès que les 10 messages ont été consommés, on sait
que : $$\text{Tous les messages ont été produits et consommés.}$$ Le
programme peut alors s'arrêter proprement, sans attente inutile.

------------------------------------------------------------------------

# Résumé Conceptuel 

-   Les producteurs créent des messages et les insèrent dans le tampon ;

-   Les consommateurs les retirent au fur et à mesure ;

-   Un moniteur affiche périodiquement les statistiques :

    -   Nombre de messages dans le tampon ;

    -   Total de messages produits ;

    -   Total de messages consommés.

-   Lorsque la consommation atteint le total attendu, le système
    s'éteint proprement.

------------------------------------------------------------------------

# Évolution des Versions 


   **Version**  **Concept principal**
  ------------- ------------------------------------------------------------------
       v1       Tampon simple, synchronisation par `wait/notify`
       v2       Quotas fixes et terminaison automatique
       v3       Tampon géré par **sémaphores** (`empty/full/mutex`)
       v4       Tampon utilisant **verrous explicites** et `Condition`
       v5       Extension avec `get(int k)` (multi-consommation)
       v6       Extension avec `put(Message,int)` (multi-exemplaires synchrones)


------------------------------------------------------------------------


## Version 3 --- Sémaphores 

L'implémentation utilise trois sémaphores :

-   `empty` : compte les places libres ;

-   `full` : compte les messages disponibles ;

-   `mutex` : assure l'exclusion mutuelle.

Ce mécanisme favorise le parallélisme en réduisant la contention par
rapport au moniteur de la version 1.

## Version 4 --- Locks et Conditions 

On remplace les moniteurs implicites par un `ReentrantLock` et deux
objets `Condition` :

-   `notFull.await()` quand le tampon est plein ;

-   `notEmpty.await()` quand il est vide.

Cette version permet un contrôle plus précis et prépare les variantes
plus avancées.

## Version 5 --- Multi-consommation 

On étend l'interface avec :

``` {.java fontsize="\\small"}
Message[] get(int k) throws InterruptedException;
```

Un consommateur peut ainsi retirer `k` messages consécutifs. Le tampon
doit bloquer jusqu'à ce que `k` messages soient disponibles.
Implémentation naïve : dans get(k), on fait while (count < k) wait();
Résultat : si la production se termine et qu’il reste moins de k messages (ex. 2 alors que k=3), la condition n’est jamais satisfaite → le consommateur attend indéfiniment. C’est un blocage « tout-ou-rien ».
L’idée simple qui corrige:

- On ajoute un état de fin : finished (booléen).
Il passe à true quand tout ce qui devait être produit a été produit (deux façons simples : soit un compteur de producteurs qui tombe à 0, soit un expectedTotal atteint).

- Dans get(k), on n’exige plus que count >= k pour sortir.
On accumule ce qui arrive au fil du temps, et si finished est vrai et qu’on ne pourra plus atteindre k, on rend le lot partiel (éventuellement vide pour signaler la fin).
## Version 6 --- Multi-exemplaires synchrones 

L'interface devient :

``` {.java fontsize="\\small"}
void put(Message m, int n) throws InterruptedException;
```

Le message est produit en `n` exemplaires :

-   Le producteur reste bloqué jusqu'à ce que les `n` exemplaires soient
    consommés ;

-   Chaque consommateur dupliqué reste bloqué jusqu'à la consommation
    totale.

Cette version permet d'étudier la synchronisation stricte entre
plusieurs threads.

------------------------------------------------------------------------

# Remarques finales 

-   Les impressions (`System.out.println`) actuelles servent au débogage
    et seront retirées dans la version finale pour éviter d'altérer les
    performances.

-   Chaque version est indépendante ; la progression se fait
    progressivement vers des mécanismes de synchronisation plus précis
    et plus généraux.
