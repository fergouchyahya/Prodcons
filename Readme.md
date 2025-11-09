# Présentation Générale {#présentation-générale .unnumbered}

L'objectif est de comprendre les mécanismes de :

-   Synchronisation entre threads producteurs et consommateurs.

-   Communication via un tampon (buffer) partagé.

-   Gestion correcte de la terminaison du système.

-   Utilisation de verrous, moniteurs et sémaphores.

# Structure du Projet {#structure-du-projet .unnumbered}

``` {language="Java"}
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
   ├─ IProdConsBuffer.java   # interface modifiée pour supporter put(Message,int)
   ├─ ProdConsBuffer.java
   ├─ Message.java
   ├─ Producer.java
   ├─ Consumer.java
   └─ TestProdCons.java
```

# Exécution du Projet {#exécution-du-projet .unnumbered}

Chaque version (`v1` à `v6`) peut être exécutée indépendamment à l'aide
de Maven.

``` {.bash language="bash"}
mvn -q clean package
mvn -q exec:java -Dexec.mainClass=prodcons.v1.TestProdCons
```

Pour exécuter une autre version, remplacez simplement `v1` par `v2`,
`v3`, etc.

# Fichier de Configuration {#fichier-de-configuration .unnumbered}

Le fichier `options.xml` définit les paramètres du système :

``` {.xml language="xml"}
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

# Principe du Fonctionnement {#principe-du-fonctionnement pour la version 2}

Chaque producteur reçoit un **quota fixe** de messages à produire.
Exemple :

::: center
   **Producteur**   **Messages à produire**
  ---------------- -------------------------
         1                     3
         2                     5
         3                     2
:::

Le total est donc : $$TOTAL = 3 + 5 + 2 = 10 \text{ messages.}$$

Cette valeur est cruciale : elle indique combien de messages existeront
dans tout le système. Dès que les 10 messages ont été consommés, on sait
que : $$\text{Tous les messages ont été produits et consommés.}$$ Le
programme peut alors s'arrêter proprement, sans attente inutile.

# Résumé Conceptuel {#résumé-conceptuel .unnumbered}

-   Les producteurs créent des messages et les insèrent dans le tampon.

-   Les consommateurs les retirent au fur et à mesure.

-   Un moniteur contrôle la progression en affichant les statistiques :

    -   Nombre de messages dans le tampon.

    -   Total de messages produits.

    -   Total de messages consommés.

-   Lorsque la consommation atteint le total attendu, le système
    s'éteint proprement.

# Évolution des Versions {#évolution-des-versions .unnumbered}

::: center
   **Version**  **Concept principal**
  ------------- -----------------------------------------
       v1       Tampon simple, pas de synchronisation
       v2       Quotas fixes, arrêt propre du système
:::

# Remarque
-   les tests utilisent des print que nous allons enlevée dans le future 
