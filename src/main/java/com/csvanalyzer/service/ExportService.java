package com.csvanalyzer.service;

import com.csvanalyzer.model.Vente;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service d'export des résultats.
 *
 * <p>Fonctionnalités :</p>
 * <ul>
 *   <li>F9 – Export des statistiques en CSV</li>
 *   <li>F9 – Export des graphiques en PNG (géré par ChartBuilder)</li>
 * </ul>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Exporte une liste de ventes vers un fichier CSV.
     *
     * @param ventes     liste des ventes à exporter
     * @param fichierDest fichier de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public void exporterVentesCSV(List<Vente> ventes, File fichierDest) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                fichierDest.toPath(), StandardCharsets.UTF_8)) {
            writer.write("date,categorie,produit,montant");
            writer.newLine();
            ventes.stream()
                    .map(v -> String.format("%s,%s,%s,%.2f",
                            v.getDate().format(DATE_FMT),
                            v.getCategorie(),
                            v.getProduit(),
                            v.getMontant()))
                    .forEach(ligne -> {
                        try {
                            writer.write(ligne);
                            writer.newLine();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    /**
     * Exporte un rapport statistique par catégorie en CSV.
     *
     * @param statsParCategorie map catégorie → somme
     * @param fichierDest       fichier de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public void exporterStatsCSV(Map<String, Double> statsParCategorie,
                                 File fichierDest) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                fichierDest.toPath(), StandardCharsets.UTF_8)) {
            writer.write("categorie,chiffre_affaires");
            writer.newLine();
            statsParCategorie.entrySet().stream()
                    .map(e -> String.format("%s,%.2f", e.getKey(), e.getValue()))
                    .forEach(ligne -> {
                        try {
                            writer.write(ligne);
                            writer.newLine();
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        }
    }

    /**
     * Exporte un rapport complet avec statistiques globales en CSV.
     *
     * @param stats           statistiques globales
     * @param parCategorie    sommes par catégorie
     * @param fichierDest     fichier de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public void exporterRapportComplet(com.csvanalyzer.model.StatistiquesResultat stats,
                                       Map<String, Double> parCategorie,
                                       File fichierDest) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                fichierDest.toPath(), StandardCharsets.UTF_8)) {

            writer.write("=== RAPPORT D'ANALYSE CSV ===");
            writer.newLine();
            writer.write("Generé le : " + java.time.LocalDate.now().format(DATE_FMT));
            writer.newLine();
            writer.newLine();

            writer.write("--- Statistiques Globales ---");
            writer.newLine();
            writer.write("indicateur,valeur");
            writer.newLine();
            writer.write("Nombre de ventes," + stats.getNombre());
            writer.newLine();
            writer.write(String.format("Chiffre d'affaires total,%.2f", stats.getSomme()));
            writer.newLine();
            writer.write(String.format("Montant moyen,%.2f", stats.getMoyenne()));
            writer.newLine();
            writer.write(String.format("Montant minimum,%.2f", stats.getMinimum()));
            writer.newLine();
            writer.write(String.format("Montant maximum,%.2f", stats.getMaximum()));
            writer.newLine();
            writer.write(String.format("Mediane,%.2f", stats.getMediane()));
            writer.newLine();
            writer.write(String.format("Ecart-type,%.2f", stats.getEcartType()));
            writer.newLine();
            writer.newLine();

            writer.write("--- Chiffre d'Affaires par Categorie ---");
            writer.newLine();
            writer.write("categorie,chiffre_affaires");
            writer.newLine();
            parCategorie.entrySet().stream()
                    .map(e -> String.format("%s,%.2f", e.getKey(), e.getValue()))
                    .forEach(ligne -> {
                        try {
                            writer.write(ligne);
                            writer.newLine();
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        }
    }
}
