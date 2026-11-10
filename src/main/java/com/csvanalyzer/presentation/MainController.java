package com.csvanalyzer.presentation;

import com.csvanalyzer.model.LectureResultat;
import com.csvanalyzer.model.StatistiquesResultat;
import com.csvanalyzer.model.Vente;
import com.csvanalyzer.service.CsvReader;
import com.csvanalyzer.service.DatabaseConfig;
import com.csvanalyzer.service.DatabaseService;
import com.csvanalyzer.service.ExportService;
import com.csvanalyzer.service.StatsService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur principal de l'application JavaFX.
 *
 * <p>Orchestre l'interface utilisateur et les interactions entre les couches
 * Service et Modèle. Toutes les opérations longues s'exécutent via
 * {@code javafx.concurrent.Task} pour ne pas bloquer le thread UI.</p>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class MainController {

    // ─── Services ─────────────────────────────────────────────────────────────
    private final CsvReader      csvReader      = new CsvReader();
    private final StatsService   statsService   = new StatsService();
    private final ExportService  exportService  = new ExportService();
    private final ChartBuilder   chartBuilder   = new ChartBuilder();
    private final DatabaseService dbService     = new DatabaseService();

    // ─── État ─────────────────────────────────────────────────────────────────
    private List<Vente> ventesOriginales   = List.of();
    private List<Vente> ventesFiltrees     = List.of();
    private Stage       primaryStage;
    private String      nomFichierCourant  = null;   // nom du fichier chargé
    private int         fichierIdCourant   = -1;     // ID en base du fichier courant
    private boolean     dbConnectee        = false;  // statut connexion DB

    // ─── Composants UI ────────────────────────────────────────────────────────
    private TableView<Vente>         tableView;
    private Label                    lblStatut;
    private Label                    lblNombre, lblSomme, lblMoyenne, lblMin, lblMax, lblMediane, lblEcartType;
    private ComboBox<String>         cbCategorie;
    private DatePicker               dpDebut, dpFin;
    private TabPane                  tabPaneGraphiques;
    private ProgressBar              progressBar;
    private TextField                tfTopN;

    /**
     * Initialise et affiche l'interface principale.
     *
     * @param stage la fenêtre principale JavaFX
     */
    public void demarrer(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("CSV Analyzer — Analyse de données de ventes");
        stage.setMinWidth(1100);
        stage.setMinHeight(750);

        // Test connexion DB au démarrage
        dbConnectee = DatabaseConfig.testerConnexion();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f6f9;");

        root.setTop(creerBandeauHaut());
        root.setLeft(creerPanneauGauche());
        root.setCenter(creerZoneCentrale());
        root.setBottom(creerBarreStatut());

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/com/csvanalyzer/style.css") != null
                ? getClass().getResource("/com/csvanalyzer/style.css").toExternalForm()
                : "");
        stage.setScene(scene);
        stage.show();
    }

    // ─── Construction UI ──────────────────────────────────────────────────────

    private Node creerBandeauHaut() {
        HBox haut = new HBox(15);
        haut.setPadding(new Insets(12, 20, 12, 20));
        haut.setAlignment(Pos.CENTER_LEFT);
        haut.setStyle("-fx-background-color: #2c3e50;");

        Label titre = new Label("📊 CSV Analyzer");
        titre.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label sousTitre = new Label("Analyse statistique et visualisation de données CSV");
        sousTitre.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCharger = new Button("📂 Charger CSV");
        btnCharger.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16;");
        btnCharger.setOnAction(e -> chargerFichier());

        Button btnExporter = new Button("💾 Exporter");
        btnExporter.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16;");
        btnExporter.setOnAction(e -> ouvrirDialogueExport());

        // ── Boutons Base de données ──
        Label lblDB = new Label();
        Button btnSauvegarderDB = new Button("🗄 Sauvegarder en DB");
        Button btnChargerDB     = new Button("📥 Charger depuis DB");
        Button btnHistoriqueDB  = new Button("📋 Historique DB");

        mettreAJourStatutDB(lblDB);

        btnSauvegarderDB.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 7 12;");
        btnSauvegarderDB.setOnAction(e -> sauvegarderEnDB());

        btnChargerDB.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 7 12;");
        btnChargerDB.setOnAction(e -> ouvrirDialogueChargerDB());

        btnHistoriqueDB.setStyle("-fx-background-color: #d35400; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 7 12;");
        btnHistoriqueDB.setOnAction(e -> afficherHistoriqueDB());

        haut.getChildren().addAll(titre, sousTitre, spacer, btnCharger, btnExporter,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                lblDB, btnSauvegarderDB, btnChargerDB, btnHistoriqueDB);
        return haut;
    }

    private Node creerPanneauGauche() {
        VBox panneau = new VBox(12);
        panneau.setPadding(new Insets(15));
        panneau.setPrefWidth(240);
        panneau.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 0 1 0 0;");

        // ── Statistiques globales ──
        Label titreStats = new Label("📈 Statistiques Globales");
        titreStats.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        lblNombre    = creerLabelStat("Nombre de ventes", "—");
        lblSomme     = creerLabelStat("CA Total (MAD)",   "—");
        lblMoyenne   = creerLabelStat("Moyenne (MAD)",    "—");
        lblMin       = creerLabelStat("Minimum (MAD)",    "—");
        lblMax       = creerLabelStat("Maximum (MAD)",    "—");
        lblMediane   = creerLabelStat("Médiane (MAD)",    "—");
        lblEcartType = creerLabelStat("Écart-type",       "—");

        Separator sep1 = new Separator();

        // ── Filtres ──
        Label titreFiltres = new Label("🔍 Filtres");
        titreFiltres.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        cbCategorie = new ComboBox<>();
        cbCategorie.setPromptText("Toutes les catégories");
        cbCategorie.setMaxWidth(Double.MAX_VALUE);

        dpDebut = new DatePicker();
        dpDebut.setPromptText("Date début");
        dpDebut.setMaxWidth(Double.MAX_VALUE);

        dpFin = new DatePicker();
        dpFin.setPromptText("Date fin");
        dpFin.setMaxWidth(Double.MAX_VALUE);

        Button btnFiltrer = new Button("Appliquer les filtres");
        btnFiltrer.setMaxWidth(Double.MAX_VALUE);
        btnFiltrer.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
        btnFiltrer.setOnAction(e -> appliquerFiltres());

        Button btnReset = new Button("Réinitialiser");
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReset.setOnAction(e -> reinitialiserFiltres());

        Separator sep2 = new Separator();

        // ── Top N ──
        Label titreTopN = new Label("🏆 Classement Top-N");
        titreTopN.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        tfTopN = new TextField("10");
        tfTopN.setPromptText("Nombre (N)");

        Button btnTopN = new Button("Afficher Top-N produits");
        btnTopN.setMaxWidth(Double.MAX_VALUE);
        btnTopN.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        btnTopN.setOnAction(e -> afficherTopN());

        panneau.getChildren().addAll(
                titreStats, lblNombre, lblSomme, lblMoyenne, lblMin, lblMax, lblMediane, lblEcartType,
                sep1,
                titreFiltres,
                new Label("Catégorie :"), cbCategorie,
                new Label("Date début :"), dpDebut,
                new Label("Date fin :"), dpFin,
                btnFiltrer, btnReset,
                sep2,
                titreTopN,
                new Label("Nombre N :"), tfTopN, btnTopN
        );

        return panneau;
    }

    private Node creerZoneCentrale() {
        TabPane tabs = new TabPane();

        // ── Tab 1 : Tableau des données ──
        Tab tabDonnees = new Tab("📋 Données");
        tabDonnees.setClosable(false);
        tabDonnees.setContent(creerTableauDonnees());

        // ── Tab 2 : Graphiques ──
        Tab tabGraphiques = new Tab("📊 Graphiques");
        tabGraphiques.setClosable(false);
        tabPaneGraphiques = new TabPane();
        tabGraphiques.setContent(tabPaneGraphiques);

        // ── Tab 3 : Stats par catégorie ──
        Tab tabCats = new Tab("📂 Par Catégorie");
        tabCats.setClosable(false);
        tabCats.setContent(creerTableauCategories());

        tabs.getTabs().addAll(tabDonnees, tabGraphiques, tabCats);
        return tabs;
    }

    @SuppressWarnings("unchecked")
    private Node creerTableauDonnees() {
        tableView = new TableView<>();
        tableView.setPlaceholder(new Label("Chargez un fichier CSV pour afficher les données"));
        tableView.setSortPolicy(tv -> true);

        TableColumn<Vente, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colDate.setPrefWidth(100);
        colDate.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<Vente, String> colCat = new TableColumn<>("Catégorie");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategorie()));
        colCat.setPrefWidth(130);

        TableColumn<Vente, String> colProduit = new TableColumn<>("Produit");
        colProduit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduit()));
        colProduit.setPrefWidth(200);

        TableColumn<Vente, Double> colMontant = new TableColumn<>("Montant (MAD)");
        colMontant.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getMontant()).asObject());
        colMontant.setPrefWidth(130);
        colMontant.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        tableView.getColumns().addAll(colDate, colCat, colProduit, colMontant);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return new VBox(tableView);
    }

    private Node creerTableauCategories() {
        TableView<Map.Entry<String, Double>> table = new TableView<>();
        table.setId("tableCats");
        table.setPlaceholder(new Label("Chargez un fichier CSV pour afficher les statistiques par catégorie"));

        TableColumn<Map.Entry<String, Double>, String> colCat = new TableColumn<>("Catégorie");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKey()));
        colCat.setPrefWidth(200);

        TableColumn<Map.Entry<String, Double>, Double> colCA = new TableColumn<>("CA Total (MAD)");
        colCA.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getValue()).asObject());
        colCA.setPrefWidth(160);
        colCA.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        table.getColumns().addAll(colCat, colCA);
        table.setId("tableCats");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return new VBox(table);
    }

    private Node creerBarreStatut() {
        HBox barre = new HBox(10);
        barre.setPadding(new Insets(5, 15, 5, 15));
        barre.setAlignment(Pos.CENTER_LEFT);
        barre.setStyle("-fx-background-color: #dfe6e9;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(150);
        progressBar.setVisible(false);

        lblStatut = new Label("Prêt — Chargez un fichier CSV pour commencer.");
        lblStatut.setStyle("-fx-text-fill: #636e72;");

        barre.getChildren().addAll(progressBar, lblStatut);
        return barre;
    }

    private Label creerLabelStat(String cle, String valeur) {
        Label lbl = new Label(cle + " : " + valeur);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50;");
        return lbl;
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    /**
     * Ouvre un FileChooser et charge le fichier CSV sélectionné (F1).
     */
    private void chargerFichier() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner un fichier CSV");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv", "*.CSV"));

        File fichier = fc.showOpenDialog(primaryStage);
        if (fichier == null) return;

        // Validation de l'en-tête avant chargement
        if (!csvReader.validerEnTete(fichier)) {
            afficherAlerte(Alert.AlertType.WARNING, "Format invalide",
                    "Le fichier ne semble pas avoir l'en-tête attendue.\n" +
                    "Format requis : date,categorie,produit,montant\n\n" +
                    "Chargement tenté quand même...");
        }

        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblStatut.setText("Chargement de : " + fichier.getName() + " ...");

        javafx.concurrent.Task<LectureResultat> tache = new javafx.concurrent.Task<>() {
            @Override
            protected LectureResultat call() throws Exception {
                return csvReader.lireFichier(fichier);
            }
        };

        tache.setOnSucceeded(evt -> {
            LectureResultat res = tache.getValue();
            ventesOriginales = res.getVentes();
            ventesFiltrees   = ventesOriginales;
            nomFichierCourant = fichier.getName();   // ← mémoriser le nom

            progressBar.setProgress(1.0);
            progressBar.setVisible(false);

            if (ventesOriginales.isEmpty()) {
                lblStatut.setText("⚠ Aucune vente valide chargée depuis : " + fichier.getName());
                afficherAlerte(Alert.AlertType.ERROR, "Fichier vide ou invalide",
                        "Aucune ligne valide n'a pu être lue.");
                return;
            }

            mettreAJourInterface();

            String msg = String.format("✅ %d ventes chargées depuis '%s'",
                    ventesOriginales.size(), fichier.getName());
            if (res.getLignesIgnorees() > 0) {
                msg += String.format(" — %d ligne(s) ignorée(s)", res.getLignesIgnorees());
                afficherAlerteErreurs(res);
            }
            lblStatut.setText(msg);
        });

        tache.setOnFailed(evt -> {
            progressBar.setVisible(false);
            lblStatut.setText("❌ Erreur lors du chargement : " + tache.getException().getMessage());
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de lecture",
                    tache.getException().getMessage());
        });

        new Thread(tache, "csv-loader").start();
    }

    /**
     * Applique les filtres sélectionnés (F8).
     */
    private void appliquerFiltres() {
        if (ventesOriginales.isEmpty()) return;

        String categorie = cbCategorie.getValue();
        LocalDate debut  = dpDebut.getValue();
        LocalDate fin    = dpFin.getValue();

        ventesFiltrees = statsService.filtrer(ventesOriginales, categorie, debut, fin);
        mettreAJourInterface();

        lblStatut.setText(String.format("🔍 Filtre appliqué : %d / %d ventes affichées",
                ventesFiltrees.size(), ventesOriginales.size()));
    }

    /**
     * Réinitialise tous les filtres.
     */
    private void reinitialiserFiltres() {
        cbCategorie.setValue(null);
        dpDebut.setValue(null);
        dpFin.setValue(null);
        ventesFiltrees = ventesOriginales;
        mettreAJourInterface();
        lblStatut.setText("✅ Filtres réinitialisés — " + ventesOriginales.size() + " ventes affichées");
    }

    /**
     * Affiche le Top-N des produits par CA (F6).
     */
    private void afficherTopN() {
        if (ventesFiltrees.isEmpty()) return;
        int n;
        try {
            n = Integer.parseInt(tfTopN.getText().trim());
            if (n <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            afficherAlerte(Alert.AlertType.WARNING, "Valeur invalide", "Entrez un entier positif pour N.");
            return;
        }

        Map<String, Double> topN = statsService.topNProduits(ventesFiltrees, n);
        BarChart<String, Number> chart = chartBuilder.creerTopNChart(
                "Top " + n + " Produits par CA", topN);

        Tab tabTopN = new Tab("🏆 Top " + n);
        tabTopN.setContent(chart);
        tabPaneGraphiques.getTabs().add(tabTopN);
        tabPaneGraphiques.getSelectionModel().select(tabTopN);
        lblStatut.setText("🏆 Top " + n + " produits affiché.");
    }

    /**
     * Met à jour tous les composants de l'interface avec les données filtrées.
     */
    private void mettreAJourInterface() {
        // Tableau des données (F3)
        ObservableList<Vente> obs = FXCollections.observableArrayList(ventesFiltrees);
        tableView.setItems(obs);

        // Statistiques globales (F4)
        StatistiquesResultat stats = statsService.calculerStatistiques(ventesFiltrees);
        lblNombre.setText("Nombre de ventes : " + stats.getNombre());
        lblSomme.setText(String.format("CA Total (MAD) : %.2f", stats.getSomme()));
        lblMoyenne.setText(String.format("Moyenne (MAD) : %.2f", stats.getMoyenne()));
        lblMin.setText(String.format("Minimum (MAD) : %.2f", stats.getMinimum()));
        lblMax.setText(String.format("Maximum (MAD) : %.2f", stats.getMaximum()));
        lblMediane.setText(String.format("Médiane (MAD) : %.2f", stats.getMediane()));
        lblEcartType.setText(String.format("Écart-type : %.2f", stats.getEcartType()));

        // ComboBox catégories (F8)
        List<String> cats = statsService.categoriesDistinctes(ventesOriginales);
        cbCategorie.setItems(FXCollections.observableArrayList(cats));

        // Tableau par catégorie (F5)
        mettreAJourTableauCategories();

        // Graphiques (F7)
        mettreAJourGraphiques();
    }

    @SuppressWarnings("unchecked")
    private void mettreAJourTableauCategories() {
        Map<String, Double> parCat = statsService.sommesParCategorie(ventesFiltrees);
        ObservableList<Map.Entry<String, Double>> items =
                FXCollections.observableArrayList(parCat.entrySet());

        // Chercher la table par ID
        primaryStage.getScene().lookup("#tableCats");
        // On rafraîchit via l'onglet
        Tab tabCat = ((TabPane) primaryStage.getScene().lookup(".tab-pane"))
                .getTabs().stream()
                .filter(t -> t.getText().contains("Catégorie"))
                .findFirst().orElse(null);

        if (tabCat != null && tabCat.getContent() instanceof VBox vbox
                && !vbox.getChildren().isEmpty()
                && vbox.getChildren().get(0) instanceof TableView<?> tv) {
            ((TableView<Map.Entry<String, Double>>) tv).setItems(items);
        }
    }

    private void mettreAJourGraphiques() {
        tabPaneGraphiques.getTabs().clear();

        Map<String, Double> parCategorie = statsService.sommesParCategorie(ventesFiltrees);
        Map<String, Double> parMois      = statsService.sommesParMois(ventesFiltrees);

        // BarChart
        BarChart<String, Number> bar = chartBuilder.creerBarChart(
                "Chiffre d'Affaires par Catégorie", parCategorie);
        Tab tabBar = new Tab("📊 Histogramme");
        tabBar.setClosable(false);
        tabBar.setContent(new ScrollPane(bar));

        // PieChart
        PieChart pie = chartBuilder.creerPieChart(
                "Répartition par Catégorie", parCategorie);
        Tab tabPie = new Tab("🥧 Secteurs");
        tabPie.setClosable(false);
        tabPie.setContent(pie);

        // LineChart
        LineChart<String, Number> line = chartBuilder.creerLineChart(
                "Évolution Mensuelle des Ventes", parMois);
        Tab tabLine = new Tab("📈 Évolution");
        tabLine.setClosable(false);
        tabLine.setContent(new ScrollPane(line));

        tabPaneGraphiques.getTabs().addAll(tabBar, tabPie, tabLine);
    }

    /**
     * Ouvre le dialogue d'export (F9).
     */
    private void ouvrirDialogueExport() {
        if (ventesFiltrees.isEmpty()) {
            afficherAlerte(Alert.AlertType.INFORMATION, "Aucune donnée", "Chargez d'abord un fichier CSV.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                "Rapport complet (CSV)",
                "Rapport complet (CSV)", "Données filtrées (CSV)", "Graphique actuel (PNG)"
        );
        dialog.setTitle("Exporter");
        dialog.setHeaderText("Choisir le type d'export");
        dialog.setContentText("Format :");

        dialog.showAndWait().ifPresent(choix -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Enregistrer");
            if (choix.contains("PNG")) {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
                fc.setInitialFileName("graphique.png");
                File f = fc.showSaveDialog(primaryStage);
                if (f != null) exporterGraphiquePNG(f);
            } else {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
                fc.setInitialFileName("rapport_export.csv");
                File f = fc.showSaveDialog(primaryStage);
                if (f != null) exporterCSV(f, choix.contains("complet"));
            }
        });
    }

    private void exporterCSV(File fichier, boolean complet) {
        try {
            if (complet) {
                StatistiquesResultat stats = statsService.calculerStatistiques(ventesFiltrees);
                Map<String, Double> parCat = statsService.sommesParCategorie(ventesFiltrees);
                exportService.exporterRapportComplet(stats, parCat, fichier);
            } else {
                exportService.exporterVentesCSV(ventesFiltrees, fichier);
            }
            lblStatut.setText("✅ Export CSV réussi : " + fichier.getName());
            afficherAlerte(Alert.AlertType.INFORMATION, "Export réussi", "Fichier sauvegardé : " + fichier.getAbsolutePath());
        } catch (IOException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur d'export", e.getMessage());
        }
    }

    private void exporterGraphiquePNG(File fichier) {
        if (tabPaneGraphiques.getTabs().isEmpty()) return;
        Tab tabActif = tabPaneGraphiques.getSelectionModel().getSelectedItem();
        if (tabActif != null && tabActif.getContent() != null) {
            try {
                Node contenu = tabActif.getContent();
                if (contenu instanceof ScrollPane sp && sp.getContent() != null) {
                    contenu = sp.getContent();
                }
                chartBuilder.exporterPNG(contenu, fichier);
                lblStatut.setText("✅ Export PNG réussi : " + fichier.getName());
                afficherAlerte(Alert.AlertType.INFORMATION, "Export PNG réussi",
                        "Image sauvegardée : " + fichier.getAbsolutePath());
            } catch (IOException e) {
                afficherAlerte(Alert.AlertType.ERROR, "Erreur d'export PNG", e.getMessage());
            }
        }
    }

    // ─── Utilitaires UI ───────────────────────────────────────────────────────

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void afficherAlerteErreurs(LectureResultat res) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Lignes ignorées");
            alert.setHeaderText(res.getLignesIgnorees() + " ligne(s) ont été ignorées.");

            String details = res.getErreursMessages().stream()
                    .limit(20)
                    .collect(Collectors.joining("\n"));
            if (res.getLignesIgnorees() > 20) details += "\n... et " + (res.getLignesIgnorees() - 20) + " autres.";

            TextArea ta = new TextArea(details);
            ta.setEditable(false);
            ta.setPrefSize(500, 200);
            alert.getDialogPane().setExpandableContent(ta);
            alert.showAndWait();
        });
    }

    // ─── Base de données ──────────────────────────────────────────────────────

    /**
     * Met à jour l'indicateur visuel de connexion DB.
     */
    private void mettreAJourStatutDB(Label lblDB) {
        if (dbConnectee) {
            lblDB.setText("🟢 DB");
            lblDB.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        } else {
            lblDB.setText("🔴 DB");
            lblDB.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    /**
     * Sauvegarde l'import courant en base de données.
     */
    private void sauvegarderEnDB() {
        if (ventesFiltrees.isEmpty() && ventesOriginales.isEmpty()) {
            afficherAlerte(Alert.AlertType.INFORMATION,
                    "Aucune donnée", "Chargez d'abord un fichier CSV.");
            return;
        }
        if (!dbConnectee) {
            afficherAlerte(Alert.AlertType.ERROR,
                    "Base de données inaccessible",
                    "Vérifiez que MariaDB/MySQL est démarré (XAMPP).");
            return;
        }

        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblStatut.setText("💾 Sauvegarde en base de données...");

        javafx.concurrent.Task<Integer> tache = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() throws Exception {
                return dbService.sauvegarderImport(
                        nomFichierCourant != null ? nomFichierCourant : "import_" + System.currentTimeMillis(),
                        "",
                        ventesOriginales,
                        0
                );
            }
        };

        tache.setOnSucceeded(e -> {
            fichierIdCourant = tache.getValue();
            progressBar.setVisible(false);
            lblStatut.setText(String.format(
                    "✅ %d ventes sauvegardées en DB (fichier_id = %d)",
                    ventesOriginales.size(), fichierIdCourant));
            afficherAlerte(Alert.AlertType.INFORMATION,
                    "Sauvegarde réussie",
                    ventesOriginales.size() + " ventes enregistrées dans MySQL.\n" +
                    "ID fichier : " + fichierIdCourant);
        });

        tache.setOnFailed(e -> {
            progressBar.setVisible(false);
            lblStatut.setText("❌ Erreur DB : " + tache.getException().getMessage());
            afficherAlerte(Alert.AlertType.ERROR,
                    "Erreur de sauvegarde DB", tache.getException().getMessage());
        });

        new Thread(tache, "db-saver").start();
    }

    /**
     * Ouvre le dialogue pour choisir un fichier importé et le charger depuis la DB.
     */
    private void ouvrirDialogueChargerDB() {
        if (!dbConnectee) {
            afficherAlerte(Alert.AlertType.ERROR, "DB inaccessible",
                    "Vérifiez que MariaDB/MySQL est démarré (XAMPP).");
            return;
        }

        try {
            java.util.List<java.util.Map<String, Object>> fichiers = dbService.listerFichiers();
            if (fichiers.isEmpty()) {
                afficherAlerte(Alert.AlertType.INFORMATION,
                        "Base vide", "Aucun import trouvé en base.\nChargez d'abord un CSV et sauvegardez-le.");
                return;
            }

            // Dialogue de choix
            Dialog<java.util.Map<String, Object>> dialog = new Dialog<>();
            dialog.setTitle("Charger depuis la base de données");
            dialog.setHeaderText("Choisissez un import à charger :");

            ListView<String> liste = new ListView<>();
            fichiers.forEach(f -> liste.getItems().add(String.format(
                    "[%d] %s — %d ventes — importé le %s",
                    f.get("id"), f.get("nom_fichier"), f.get("nb_lignes"), f.get("date_import"))));
            liste.setPrefSize(500, 200);
            liste.getSelectionModel().selectFirst();

            dialog.getDialogPane().setContent(liste);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    int idx = liste.getSelectionModel().getSelectedIndex();
                    return idx >= 0 ? fichiers.get(idx) : null;
                }
                return null;
            });

            dialog.showAndWait().ifPresent(fichier -> {
                int id  = (int) fichier.get("id");
                String nom = (String) fichier.get("nom_fichier");
                chargerDepuisDB(id, nom);
            });

        } catch (java.sql.SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur DB", e.getMessage());
        }
    }

    /**
     * Charge les ventes d'un fichier depuis la DB et met à jour l'interface.
     */
    private void chargerDepuisDB(int fichierId, String nomFichier) {
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblStatut.setText("📥 Chargement depuis la base de données...");

        javafx.concurrent.Task<java.util.List<Vente>> tache = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<Vente> call() throws Exception {
                return dbService.chargerVentes(fichierId);
            }
        };

        tache.setOnSucceeded(e -> {
            ventesOriginales   = tache.getValue();
            ventesFiltrees     = ventesOriginales;
            fichierIdCourant   = fichierId;
            nomFichierCourant  = nomFichier;

            progressBar.setVisible(false);
            mettreAJourInterface();
            lblStatut.setText(String.format(
                    "📥 %d ventes chargées depuis la DB (%s)",
                    ventesOriginales.size(), nomFichier));
        });

        tache.setOnFailed(e -> {
            progressBar.setVisible(false);
            afficherAlerte(Alert.AlertType.ERROR,
                    "Erreur de chargement DB", tache.getException().getMessage());
        });

        new Thread(tache, "db-loader").start();
    }

    /**
     * Affiche l'historique des fichiers importés en DB dans une fenêtre dédiée.
     */
    private void afficherHistoriqueDB() {
        if (!dbConnectee) {
            afficherAlerte(Alert.AlertType.ERROR, "DB inaccessible",
                    "Vérifiez que MariaDB/MySQL est démarré.");
            return;
        }

        try {
            java.util.List<java.util.Map<String, Object>> fichiers = dbService.listerFichiers();

            // Tableau des imports
            @SuppressWarnings("unchecked")
            TableView<java.util.Map<String, Object>> table = new TableView<>();

            TableColumn<java.util.Map<String, Object>, String> colId
                    = new TableColumn<>("ID");
            colId.setCellValueFactory(c ->
                    new SimpleStringProperty(String.valueOf(c.getValue().get("id"))));
            colId.setPrefWidth(50);

            TableColumn<java.util.Map<String, Object>, String> colNom
                    = new TableColumn<>("Fichier");
            colNom.setCellValueFactory(c ->
                    new SimpleStringProperty((String) c.getValue().get("nom_fichier")));
            colNom.setPrefWidth(200);

            TableColumn<java.util.Map<String, Object>, String> colNbL
                    = new TableColumn<>("Ventes");
            colNbL.setCellValueFactory(c ->
                    new SimpleStringProperty(String.valueOf(c.getValue().get("nb_lignes"))));
            colNbL.setPrefWidth(70);

            TableColumn<java.util.Map<String, Object>, String> colDate
                    = new TableColumn<>("Date import");
            colDate.setCellValueFactory(c ->
                    new SimpleStringProperty((String) c.getValue().get("date_import")));
            colDate.setPrefWidth(180);

            table.getColumns().addAll(colId, colNom, colNbL, colDate);
            table.setItems(FXCollections.observableArrayList(fichiers));

            // Bouton supprimer
            Button btnSuppr = new Button("🗑 Supprimer sélection");
            btnSuppr.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
            btnSuppr.setOnAction(ev -> {
                java.util.Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Supprimer l'import '" + sel.get("nom_fichier") + "' et toutes ses ventes ?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            try {
                                dbService.supprimerImport((int) sel.get("id"));
                                table.getItems().remove(sel);
                            } catch (java.sql.SQLException ex) {
                                afficherAlerte(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                            }
                        }
                    });
                }
            });

            VBox contenu = new VBox(10, new Label("Imports enregistrés en base :"), table, btnSuppr);
            contenu.setPadding(new Insets(15));

            Stage fHistorique = new Stage();
            fHistorique.setTitle("🗄 Historique des imports DB");
            fHistorique.setScene(new Scene(contenu, 550, 350));
            fHistorique.initOwner(primaryStage);
            fHistorique.show();

        } catch (java.sql.SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur DB", e.getMessage());
        }
    }
}
