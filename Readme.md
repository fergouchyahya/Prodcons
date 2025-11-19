
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


# Présentation générale

Ce dépôt rassemble plusieurs implémentations didactiques du problème
Producteur–Consommateur (package `prodcons`). Chaque version (v1..v7)
illustre une technique de synchronisation différente et compare deux
approches de terminaison : l'approche historique ("originale") et une
approche où la responsabilité de la terminaison est confiée au tampon
("buffer‑centrée").

Objectifs de ce README
- donner, pour chaque version, une description claire du principe et
    des choix de synchronisation ;
- documenter les deux variantes (originale vs buffer‑centrée) et leur
    contrat ;
- lister les fichiers présents par version (y compris v6 et v7) ;
- expliquer comment compiler, exécuter et configurer les tests.

---

## Arborescence (extrait complet v1..v7)

```text
prodcons/
├─ resources/
│  └─ prodcons/options.xml
├─ v1/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v2/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v3/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v4/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v5/
│  ├─ IProdConsBuffer.java   # interface étendue : get(int k)
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v6/
│  ├─ IProdConsBuffer.java   # interface avec put(m, n)
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Log.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
└─ v7/
     ├─ IProdConsBuffer.java
     ├─ ProdConsBuffer.java
     ├─ Message.java
     ├─ Log.java
     ├─ Producer.java
     ├─ Consumer.java
     ├─ TaskExecutor.java      # exécuteur dynamique (v7)
     ├─ TestProdCons.java
     └─ TestTaskExecutor.java
```

Les fichiers `options.xml` contiennent les paramètres utilisés par
les tests (nombre de producteurs/consommateurs, tailles, délais,
paramètres propres à v5/v6, ...).

---

## Comment compiler et lancer

```bash
mvn -q clean package
```

Exemples :

```bash
java -cp target/classes prodcons.v3.TestProdCons
java -cp target/classes prodcons.v5.TestProdCons
java -cp target/classes prodcons.v7.TestTaskExecutor
```

Pour changer les paramètres, éditez `prodcons/resources/prodcons/options.xml`.

---

## Contrat commun (buffer‑centré)

Les implémentations buffer‑centrées partagent le même contrat utile à
connaître :

- `void setProducersCount(int n)` (optionnel) : initialiser le nombre
    de producteurs attendus.
- `void producerDone()` : appelé par chaque producteur en `finally`
    pour indiquer la fin de sa production.
- `boolean isClosed()` : vrai quand tous les producteurs ont appelé
    `producerDone()`.
- `Message get()` : retourne le prochain message ou :
    - se bloque si vide et des producteurs restent ;
    - retourne `null` si vide et `isClosed()==true` (signal de fin).
- `Message[] get(int k)` (v5) :
    - renvoie exactement `k` messages si disponibles ;
    - renvoie le reste (taille < k) si `isClosed()==true` et moins de
        `k` messages subsistent ;
    - se bloque sinon.

Cette API permet aux consommateurs de terminer sans intervention
externe (tests / interruptions).

---

## Détails par version (principes et variantes)

Pour chaque version je présente : le principe technique, la variante
originale (historique) et la variante buffer‑centrée (recommandée).

### v1 — Moniteurs Java (`synchronized`)

Principe
- Tampon implémenté avec `synchronized`, `wait()` et `notifyAll()`.

Variante originale
- Comportement didactique : production/consommation avec réveils
    globaux. Les tests pouvaient interrompre les consommateurs pour
    forcer l'arrêt.

Variante buffer‑centrée
- Ajouter `producerDone()`/`setProducersCount()` et `isClosed()` :
    les consommateurs quittent lorsque `get()` retourne `null`.

Utilisation
- `java -cp target/classes prodcons.v1.TestProdCons`

---

### v2 — Moniteurs + quotas (terminaison déterministe)

Principe
- Chaque producteur reçoit un quota ; la somme des quotas est le
    TOTAL attendu.

Variante originale
- Le test attribue les quotas et orchestre la fin (join + actions
    explicites).

Variante buffer‑centrée
- Buffer avec `producerDone()` décrémente un compteur. Lorsque le
    tampon est vide et fermé, `get()` renvoie `null` et les consommateurs
    se terminent proprement.

Utilisation
- `java -cp target/classes prodcons.v2.TestProdCons`

---

### v3 — Sémaphores (`empty`, `full`, `mutex`)

Principe
- Coordination via sémaphores pour contrôler cases libres (`empty`),
    messages prêts (`full`) et exclusion (`mutex`).

Variante originale
- Terminaison pilotée par le test (approche historique).

Variante buffer‑centrée
- On combine sémaphores avec un compteur de producteurs et
    `producerDone()` : `get()` retourne `null` quand la production est
    terminée et le tampon vide.

Utilisation
- `java -cp target/classes prodcons.v3.TestProdCons`

---

### v4 — `ReentrantLock` + `Condition`

Principe
- Synchronisation plus fine via `ReentrantLock` et `Condition`
    (`notFull`/`notEmpty`).

Variante originale
- Fin gérée par le test.

Variante buffer‑centrée
- Implémentation du contrat `producerDone()` / `isClosed()` ;
    `get()` renvoie `null` pour signaler la fin.

Utilisation
- `java -cp target/classes prodcons.v4.TestProdCons`

---

### v5 — Consommation par lots (`get(int k)`) — modèle déjà prêt pour
la terminaison

Principe
- Un consommateur peut demander un lot de `k` messages avec
    `Message[] get(int k)`.

Variante originale (conception historique)
- v5 a été conçue pour accepter un `expectedTotal` : si la production
    se termine avant qu'un lot complet ne soit disponible, `get(k)`
    renvoie les éléments restants (taille < k) — comportement sûr pour
    la terminaison.

Variante buffer‑centrée (uniformisation)
- On peut remplacer `expectedTotal` par `setProducersCount()`/
    `producerDone()` ; `get(k)` renvoie un lot partiel si `isClosed()`.

Utilisation
- `java -cp target/classes prodcons.v5.TestProdCons`

---

### v6 — Multi‑exemplaires synchrones (`put(Message m, int n)`)

Principe
- Le producteur peut déposer `n` exemplaires du même message. Les
    slots du tampon contiennent un compteur d'exemplaires et une
    condition locale pour coordonner les consommateurs.

Variante originale
- Montre la coordination locale entre consommateurs ; la terminaison
    était gérée par les tests.

Variante buffer‑centrée
- Ajouter `producerDone()` global et laisser les slots gérer la
    synchronisation locale. Les consommateurs quittent proprement quand
    tous les exemplaires sont traités et que la production est close.

Utilisation
- `java -cp target/classes prodcons.v6.TestProdCons`

---

### v7 — Variantes avancées et `TaskExecutor`

Principe
- Tests d'optimisation et composant additionnel `TaskExecutor` qui
    illustre un pool dynamique.

Variante originale
- Implémentation historique des producteurs/consommateurs (utile
    pour comparaison).

Variante buffer‑centrée
- `producerDone()` / `isClosed()` pour la terminaison globale.

TaskExecutor
- Un exécuteur simple démontrant :
    - création dynamique de workers jusqu'à `maxWorkers` ;
    - workers s'arrêtant après 3s d'inactivité ;
    - utile pour déléguer des tâches CPU sans garder un pool fixe.

Utilisation
- `java -cp target/classes prodcons.v7.TestProdCons`
- `java -cp target/classes prodcons.v7.TestTaskExecutor`

---

## Observabilité et invariants

- Chaque `TestProdCons` affiche périodiquement : `nmsg` (nombre de
    slots/messages actifs), `tot` (total produit) et `consumed` (total
    consommé).
- Condition de terminaison correcte : tampon vide + `isClosed()==true`
    + compteurs cohérents.

---

## Bonnes pratiques et prochaines étapes

- Conserver la variante historique en commentaire est pratique pour
    l'enseignement ; si vous préférez un repo plus propre, je peux
    déplacer ces fichiers dans `archived/`.
- Ajouter des tests unitaires (JUnit) qui vérifient automatiquement la
    terminaison et les invariants sur chaque version est recommandé.
- Si vous souhaitez une API uniforme, on peut migrer v5 pour utiliser
    `producerDone()` à la place de `expectedTotal`.

---

## Conclusion

Ce projet sert d'outil pédagogique : il montre la progression des
patterns de synchronisation en Java et compare deux approches de
gestion de la terminaison. La variante buffer‑centrée fournit une
séparation des responsabilités et des tests plus simples.
