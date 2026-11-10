package com.csvanalyzer.service;

import com.csvanalyzer.model.LectureResultat;
import com.csvanalyzer.model.Vente;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service de lecture et de validation des fichiers CSV.
 *
 * <p>Format attendu (UTF-8, séparateur virgule) :</p>
 * <pre>date,categorie,produit,montant</pre>
 *
 * <p>Utilise l'API {@code java.nio.file.Files.lines} avec l'API Streams Java
 * pour un traitement fonctionnel ligne par ligne (F1, F2).</p>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class CsvReader {

    /** Formats de dates acceptés */
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    /**
     * Lit et valide un fichier CSV, retourne un {@link LectureResultat}.
     *
     * @param fichier le fichier CSV à lire
     * @return résultat contenant les ventes valides et les erreurs
     * @throws IOException si le fichier n'est pas lisible
     */
    public LectureResultat lireFichier(File fichier) throws IOException {
        List<Vente>  ventes   = new ArrayList<>();
        List<String> erreurs  = new ArrayList<>();
        AtomicInteger numLigne = new AtomicInteger(0);

        try (Stream<String> lignes = Files.lines(fichier.toPath(), StandardCharsets.UTF_8)) {
            lignes
                .peek(l -> numLigne.incrementAndGet())
                .filter(l -> numLigne.get() > 1)          // ignore l'en-tête (ligne 1)
                .filter(l -> !l.isBlank())                 // ignore lignes vides
                .forEach(ligne -> {
                    int num = numLigne.get();
                    try {
                        Vente v = validerLigne(ligne, num);
                        synchronized (ventes) { ventes.add(v); }
                    } catch (IllegalArgumentException e) {
                        synchronized (erreurs) {
                            erreurs.add("Ligne " + num + " : " + e.getMessage());
                        }
                    }
                });
        }

        return new LectureResultat(ventes, erreurs.size(), erreurs);
    }

    /**
     * Valide et parse une ligne CSV en objet {@link Vente}.
     *
     * @param ligne  la ligne brute
     * @param numLigne numéro de ligne (pour le message d'erreur)
     * @return l'objet Vente correspondant
     * @throws IllegalArgumentException si la ligne est invalide
     */
    private Vente validerLigne(String ligne, int numLigne) {
        String[] champs = ligne.split(",", -1);

        if (champs.length < 4) {
            throw new IllegalArgumentException(
                "Nombre de colonnes insuffisant (" + champs.length + " trouvées, 4 attendues)");
        }

        String dateStr     = champs[0].trim();
        String categorie   = champs[1].trim();
        String produit     = champs[2].trim();
        String montantStr  = champs[3].trim();

        if (dateStr.isEmpty() || categorie.isEmpty() || produit.isEmpty() || montantStr.isEmpty()) {
            throw new IllegalArgumentException("Champ(s) obligatoire(s) manquant(s)");
        }

        LocalDate date = parseDate(dateStr);
        if (date == null) {
            throw new IllegalArgumentException("Format de date invalide : '" + dateStr + "'");
        }

        double montant;
        try {
            montant = Double.parseDouble(montantStr.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Montant non numérique : '" + montantStr + "'");
        }

        if (montant < 0) {
            throw new IllegalArgumentException("Montant négatif : " + montant);
        }

        return new Vente(date, categorie, produit, montant);
    }

    /**
     * Tente de parser une date avec les différents formats supportés.
     *
     * @param dateStr chaîne de date
     * @return {@link LocalDate} ou {@code null} si aucun format ne convient
     */
    private LocalDate parseDate(String dateStr) {
        return DATE_FORMATTERS.stream()
                .map(fmt -> {
                    try { return LocalDate.parse(dateStr, fmt); }
                    catch (DateTimeParseException e) { return null; }
                })
                .filter(d -> d != null)
                .findFirst()
                .orElse(null);
    }

    /**
     * Vérifie si un fichier semble être un CSV valide (en-tête requise).
     *
     * @param fichier le fichier à vérifier
     * @return true si l'en-tête contient les colonnes attendues
     */
    public boolean validerEnTete(File fichier) {
        try (BufferedReader br = Files.newBufferedReader(fichier.toPath(), StandardCharsets.UTF_8)) {
            String entete = br.readLine();
            if (entete == null) return false;
            String enteteNorm = entete.toLowerCase().replace(" ", "");
            return enteteNorm.contains("date") &&
                   enteteNorm.contains("categorie") &&
                   enteteNorm.contains("produit") &&
                   enteteNorm.contains("montant");
        } catch (IOException e) {
            return false;
        }
    }
}
