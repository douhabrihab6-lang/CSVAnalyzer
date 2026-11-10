# CSV Analyzer
## Application Java d'analyse de données CSV : statistiques et visualisation

**Auteurs :** Rihab Douhab & Soumia El Halla  
**Établissement :** EMSI Moulay Youssef — 4IIR, Groupe 2  
**Version :** 1.0.0 | **Date :** Novembre 2024

---

## 📋 Description

Application de bureau Java/JavaFX permettant de charger des fichiers CSV de ventes,
de calculer automatiquement des indicateurs statistiques grâce à l'**API Streams**,
et de les visualiser sous forme de graphiques interactifs.

---

## 🏗 Architecture

```
src/main/java/com/csvanalyzer/
├── model/
│   ├── Vente.java               ← Entité métier (date, categorie, produit, montant)
│   ├── LectureResultat.java     ← Résultat de lecture CSV (ventes + erreurs)
│   └── StatistiquesResultat.java← Indicateurs statistiques calculés
├── service/
│   ├── CsvReader.java           ← Lecture + validation CSV (java.nio, Streams)
│   ├── StatsService.java        ← Calculs statistiques (100% API Streams)
│   └── ExportService.java       ← Export CSV et rapports
└── presentation/
    ├── MainApp.java             ← Point d'entrée JavaFX
    ├── MainController.java      ← Contrôleur principal (IHM)
    └── ChartBuilder.java        ← Constructeur de graphiques JavaFX
```

---

## ⚙️ Technologies

| Domaine | Technologie |
|---------|------------|
| Langage | Java 21 (Streams, Lambdas, Records) |
| Interface | JavaFX 21 (TableView, Charts, FileChooser) |
| Build | Maven 3.9+ |
| Tests | JUnit 5 |
| Lecture CSV | java.nio.file.Files.lines() |
| Calculs | java.util.stream (groupingBy, summaryStatistics) |
| Export PNG | javafx.embed.swing.SwingFXUtils |

---

## 🚀 Lancement

### Prérequis
- Java 21+
- Maven 3.9+

### Compilation et exécution
```bash
# Compiler
mvn clean compile

# Lancer les tests
mvn test

# Lancer l'application
mvn javafx:run
```

---

## 📊 Fonctionnalités (F1–F9)

| Réf. | Fonctionnalité | Description |
|------|---------------|-------------|
| F1 | Chargement CSV | FileChooser + chargement asynchrone |
| F2 | Validation | Détection lignes invalides, message d'erreur |
| F3 | Affichage | TableView triable par colonne |
| F4 | Stats globales | Moyenne, min, max, somme, médiane, écart-type |
| F5 | Stats catégorie | groupingBy (CA, count, moyenne par catégorie) |
| F6 | Classement | Top-N produits par CA |
| F7 | Graphiques | BarChart, PieChart, LineChart |
| F8 | Filtrage | Par catégorie et/ou période |
| F9 | Export | CSV (rapport complet) + PNG (graphiques) |

---

## 📁 Format du fichier CSV

```csv
date,categorie,produit,montant
2024-01-10,Électronique,Laptop Pro,2500.00
2024-02-05,Vêtements,Jean Slim,135.00
```

**Formats de date acceptés :** `yyyy-MM-dd`, `dd/MM/yyyy`, `MM/dd/yyyy`, `dd-MM-yyyy`

---

## 🧪 Tests unitaires

```bash
mvn test
```

Couverture :
- Statistiques globales (nombre, somme, moyenne, min, max, médiane)
- Statistiques par catégorie (groupingBy)
- Évolution mensuelle
- Top-N classement
- Filtrage par catégorie, période, combiné
- Lecture CSV valide / invalide
- Formats de date multiples
- Rejet des montants négatifs

---

## 📂 Jeux de données

| Fichier | Description |
|---------|-------------|
| `data/ventes_test.csv` | 40 ventes valides sur 5 mois |
| `data/ventes_invalide.csv` | Fichier avec lignes volontairement invalides |
