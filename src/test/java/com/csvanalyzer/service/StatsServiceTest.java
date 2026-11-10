package com.csvanalyzer.service;

import com.csvanalyzer.model.LectureResultat;
import com.csvanalyzer.model.StatistiquesResultat;
import com.csvanalyzer.model.Vente;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires JUnit 5 pour {@link StatsService} et {@link CsvReader}.
 *
 * <p>Critères de validation :</p>
 * <ul>
 *   <li>Chargement correct d'un CSV valide et rejet d'un CSV invalide</li>
 *   <li>Résultats statistiques conformes à un calcul de référence</li>
 *   <li>Couverture des services de calcul</li>
 * </ul>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
@DisplayName("Tests — StatsService & CsvReader")
class StatsServiceTest {

    private StatsService statsService;
    private List<Vente>  ventes;

    @BeforeEach
    void setUp() {
        statsService = new StatsService();
        ventes = List.of(
                new Vente(LocalDate.of(2024, 1, 10), "Électronique", "Laptop",    1500.0),
                new Vente(LocalDate.of(2024, 1, 15), "Électronique", "Téléphone",  800.0),
                new Vente(LocalDate.of(2024, 2, 5),  "Vêtements",    "Manteau",    350.0),
                new Vente(LocalDate.of(2024, 2, 20), "Vêtements",    "Jean",       120.0),
                new Vente(LocalDate.of(2024, 3, 1),  "Alimentation", "Café",        50.0)
        );
    }

    // ─── Tests StatsService ───────────────────────────────────────────────────

    @Test
    @DisplayName("F4 — Nombre de ventes correct")
    void testNombreVentes() {
        StatistiquesResultat stats = statsService.calculerStatistiques(ventes);
        assertEquals(5, stats.getNombre(), "Le nombre de ventes doit être 5");
    }

    @Test
    @DisplayName("F4 — Somme totale correcte")
    void testSommeTotale() {
        StatistiquesResultat stats = statsService.calculerStatistiques(ventes);
        assertEquals(2820.0, stats.getSomme(), 0.01, "La somme doit être 2820.0");
    }

    @Test
    @DisplayName("F4 — Moyenne correcte")
    void testMoyenne() {
        StatistiquesResultat stats = statsService.calculerStatistiques(ventes);
        assertEquals(564.0, stats.getMoyenne(), 0.01, "La moyenne doit être 564.0");
    }

    @Test
    @DisplayName("F4 — Minimum correct")
    void testMinimum() {
        StatistiquesResultat stats = statsService.calculerStatistiques(ventes);
        assertEquals(50.0, stats.getMinimum(), 0.01, "Le minimum doit être 50.0");
    }

    @Test
    @DisplayName("F4 — Maximum correct")
    void testMaximum() {
        StatistiquesResultat stats = statsService.calculerStatistiques(ventes);
        assertEquals(1500.0, stats.getMaximum(), 0.01, "Le maximum doit être 1500.0");
    }

    @Test
    @DisplayName("F4 — Médiane correcte")
    void testMediane() {
        double mediane = statsService.calculerMediane(ventes);
        // Montants triés : 50, 120, 350, 800, 1500 → médiane = 350
        assertEquals(350.0, mediane, 0.01, "La médiane doit être 350.0");
    }

    @Test
    @DisplayName("F4 — Statistiques sur liste vide → zéros")
    void testListeVide() {
        StatistiquesResultat stats = statsService.calculerStatistiques(List.of());
        assertEquals(0, stats.getNombre());
        assertEquals(0.0, stats.getSomme());
    }

    @Test
    @DisplayName("F5 — Sommes par catégorie correctes")
    void testSommesParCategorie() {
        Map<String, Double> parCat = statsService.sommesParCategorie(ventes);
        assertEquals(2300.0, parCat.get("Électronique"), 0.01);
        assertEquals(470.0,  parCat.get("Vêtements"),    0.01);
        assertEquals(50.0,   parCat.get("Alimentation"), 0.01);
    }

    @Test
    @DisplayName("F5 — Nombre de catégories distinctes")
    void testCategoriesDistinctes() {
        List<String> cats = statsService.categoriesDistinctes(ventes);
        assertEquals(3, cats.size());
        assertTrue(cats.contains("Électronique"));
        assertTrue(cats.contains("Vêtements"));
        assertTrue(cats.contains("Alimentation"));
    }

    @Test
    @DisplayName("F5 — Évolution mensuelle correcte")
    void testEvolutionMensuelle() {
        Map<String, Double> parMois = statsService.sommesParMois(ventes);
        assertEquals(2300.0, parMois.get("2024-01"), 0.01);
        assertEquals(470.0,  parMois.get("2024-02"), 0.01);
        assertEquals(50.0,   parMois.get("2024-03"), 0.01);
    }

    @Test
    @DisplayName("F6 — Top N ventes")
    void testTopNVentes() {
        List<Vente> top3 = statsService.topNVentes(ventes, 3);
        assertEquals(3, top3.size());
        assertEquals(1500.0, top3.get(0).getMontant(), 0.01, "1er = Laptop 1500");
        assertEquals(800.0,  top3.get(1).getMontant(), 0.01, "2ème = Téléphone 800");
        assertEquals(350.0,  top3.get(2).getMontant(), 0.01, "3ème = Manteau 350");
    }

    @Test
    @DisplayName("F8 — Filtrage par catégorie")
    void testFiltrerParCategorie() {
        List<Vente> electroniques = statsService.filtrerParCategorie(ventes, "Électronique");
        assertEquals(2, electroniques.size());
        assertTrue(electroniques.stream().allMatch(v -> v.getCategorie().equals("Électronique")));
    }

    @Test
    @DisplayName("F8 — Filtrage par catégorie insensible à la casse")
    void testFiltrerParCategorieCasse() {
        List<Vente> res = statsService.filtrerParCategorie(ventes, "ÉLECTRONIQUE");
        assertEquals(2, res.size());
    }

    @Test
    @DisplayName("F8 — Filtrage par période")
    void testFiltrerParPeriode() {
        List<Vente> janv = statsService.filtrerParPeriode(ventes,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        assertEquals(2, janv.size());
    }

    @Test
    @DisplayName("F8 — Filtrage combiné catégorie + période")
    void testFiltrerCombine() {
        List<Vente> res = statsService.filtrer(ventes,
                "Vêtements", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28));
        assertEquals(2, res.size());
    }

    // ─── Tests CsvReader ─────────────────────────────────────────────────────

    @Test
    @DisplayName("F1 — Lecture d'un CSV valide")
    void testLectureCsvValide() throws IOException {
        File csv = creerFichierTemp(
                "date,categorie,produit,montant\n" +
                "2024-01-10,Electronique,Laptop,1500.00\n" +
                "2024-02-05,Vetements,Jean,120.50\n"
        );
        CsvReader reader = new CsvReader();
        LectureResultat res = reader.lireFichier(csv);
        assertEquals(2, res.getVentes().size());
        assertEquals(0, res.getLignesIgnorees());
    }

    @Test
    @DisplayName("F2 — Rejet d'une ligne avec montant invalide")
    void testLectureAvecErreur() throws IOException {
        File csv = creerFichierTemp(
                "date,categorie,produit,montant\n" +
                "2024-01-10,Electronique,Laptop,1500.00\n" +
                "2024-02-05,Vetements,Jean,ABC\n"   // montant invalide
        );
        CsvReader reader = new CsvReader();
        LectureResultat res = reader.lireFichier(csv);
        assertEquals(1, res.getVentes().size(), "Une seule ligne valide");
        assertEquals(1, res.getLignesIgnorees(), "Une ligne ignorée");
        assertFalse(res.getErreursMessages().isEmpty());
    }

    @Test
    @DisplayName("F2 — Rejet d'une ligne incomplète (moins de 4 colonnes)")
    void testLectureColonnesInsuffisantes() throws IOException {
        File csv = creerFichierTemp(
                "date,categorie,produit,montant\n" +
                "2024-01-10,Electronique\n"   // seulement 2 colonnes
        );
        CsvReader reader = new CsvReader();
        LectureResultat res = reader.lireFichier(csv);
        assertEquals(0, res.getVentes().size());
        assertEquals(1, res.getLignesIgnorees());
    }

    @Test
    @DisplayName("F2 — Formats de date multiples supportés")
    void testFormatsDate() throws IOException {
        File csv = creerFichierTemp(
                "date,categorie,produit,montant\n" +
                "2024-01-10,Cat,Prod1,100\n" +
                "10/01/2024,Cat,Prod2,200\n" +
                "01/10/2024,Cat,Prod3,300\n"
        );
        CsvReader reader = new CsvReader();
        LectureResultat res = reader.lireFichier(csv);
        assertEquals(3, res.getVentes().size(), "Les 3 formats de date doivent être acceptés");
    }

    @Test
    @DisplayName("F2 — Rejet d'un montant négatif")
    void testMontantNegatif() throws IOException {
        File csv = creerFichierTemp(
                "date,categorie,produit,montant\n" +
                "2024-01-10,Cat,Prod,-50.0\n"
        );
        CsvReader reader = new CsvReader();
        LectureResultat res = reader.lireFichier(csv);
        assertEquals(0, res.getVentes().size());
        assertEquals(1, res.getLignesIgnorees());
    }

    // ─── Utilitaire ───────────────────────────────────────────────────────────

    private File creerFichierTemp(String contenu) throws IOException {
        File f = File.createTempFile("test_csv_", ".csv");
        f.deleteOnExit();
        Files.writeString(f.toPath(), contenu, StandardCharsets.UTF_8);
        return f;
    }
}
