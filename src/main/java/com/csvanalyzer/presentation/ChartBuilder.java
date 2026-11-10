package com.csvanalyzer.presentation;

import com.csvanalyzer.model.Vente;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Constructeur de graphiques JavaFX.
 *
 * <p>Génère les types de graphiques demandés (F7) :</p>
 * <ul>
 *   <li>BarChart – histogramme des ventes par catégorie</li>
 *   <li>PieChart – répartition en secteurs</li>
 *   <li>LineChart – évolution temporelle</li>
 * </ul>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class ChartBuilder {

    /**
     * Crée un BarChart (histogramme) des sommes par catégorie.
     *
     * @param titre         titre du graphique
     * @param donnees       map catégorie → valeur
     * @return BarChart prêt à l'affichage
     */
    public BarChart<String, Number> creerBarChart(String titre, Map<String, Double> donnees) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Catégorie");
        yAxis.setLabel("Montant (MAD)");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(titre);
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("CA par catégorie");

        donnees.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> serie.getData().add(
                        new XYChart.Data<>(e.getKey(), e.getValue())));

        chart.getData().add(serie);
        chart.setPrefSize(700, 400);
        return chart;
    }

    /**
     * Crée un PieChart (secteurs) de la répartition par catégorie.
     *
     * @param titre   titre du graphique
     * @param donnees map catégorie → valeur
     * @return PieChart prêt à l'affichage
     */
    public PieChart creerPieChart(String titre, Map<String, Double> donnees) {
        PieChart chart = new PieChart();
        chart.setTitle(titre);
        chart.setAnimated(false);

        donnees.forEach((cat, val) ->
                chart.getData().add(new PieChart.Data(
                        String.format("%s (%.1f%%)", cat,
                                val / donnees.values().stream().mapToDouble(d -> d).sum() * 100),
                        val)));

        chart.setPrefSize(700, 400);
        return chart;
    }

    /**
     * Crée un LineChart (courbe) de l'évolution mensuelle des ventes.
     *
     * @param titre   titre du graphique
     * @param donnees map mois → montant total (clés triées chronologiquement)
     * @return LineChart prêt à l'affichage
     */
    public LineChart<String, Number> creerLineChart(String titre, Map<String, Double> donnees) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Mois");
        yAxis.setLabel("Montant (MAD)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(titre);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Évolution mensuelle");

        donnees.forEach((mois, val) ->
                serie.getData().add(new XYChart.Data<>(mois, val)));

        chart.getData().add(serie);
        chart.setPrefSize(700, 400);
        return chart;
    }

    /**
     * Crée un BarChart pour le Top-N des produits.
     *
     * @param titre   titre du graphique
     * @param donnees map produit → CA
     * @return BarChart
     */
    public BarChart<String, Number> creerTopNChart(String titre, Map<String, Double> donnees) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Produit");
        yAxis.setLabel("Chiffre d'Affaires (MAD)");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(titre);
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        donnees.forEach((p, v) -> serie.getData().add(new XYChart.Data<>(p, v)));

        chart.getData().add(serie);
        chart.setPrefSize(700, 400);
        return chart;
    }

    /**
     * Exporte un nœud JavaFX en fichier PNG (F9).
     *
     * @param chart       le graphique à exporter
     * @param fichierDest fichier PNG de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public void exporterPNG(javafx.scene.Node chart, File fichierDest) throws IOException {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        WritableImage image = chart.snapshot(params, null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", fichierDest);
    }
}
