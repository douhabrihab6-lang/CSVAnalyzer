package com.csvanalyzer.service;

import com.csvanalyzer.model.StatistiquesResultat;
import com.csvanalyzer.model.Vente;

import java.util.*;
import java.util.stream.*;
import java.util.function.Function;

/**
 * Service de calcul statistique utilisant exclusivement l'API Streams de Java.
 *
 * <p>Fonctionnalités couvertes :</p>
 * <ul>
 *   <li>F4 – Statistiques globales (moyenne, min, max, somme, nombre, médiane, écart-type)</li>
 *   <li>F5 – Statistiques par catégorie (groupingBy)</li>
 *   <li>F6 – Classement Top-N</li>
 *   <li>F8 – Filtrage par catégorie et période</li>
 * </ul>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class StatsService {

    // ─── F4 : Statistiques globales ───────────────────────────────────────────

    /**
     * Calcule les statistiques descriptives globales sur une liste de ventes.
     *
     * @param ventes liste des ventes
     * @return {@link StatistiquesResultat} complet
     */
    public StatistiquesResultat calculerStatistiques(List<Vente> ventes) {
        if (ventes == null || ventes.isEmpty()) {
            return new StatistiquesResultat(0, 0, 0, 0, 0, 0, 0);
        }

        DoubleSummaryStatistics stats = ventes.stream()
                .mapToDouble(Vente::getMontant)
                .summaryStatistics();

        double moyenne   = stats.getAverage();
        double mediane   = calculerMediane(ventes);
        double ecartType = calculerEcartType(ventes, moyenne);

        return new StatistiquesResultat(
                stats.getCount(),
                stats.getSum(),
                moyenne,
                stats.getMin(),
                stats.getMax(),
                mediane,
                ecartType
        );
    }

    /**
     * Calcule la médiane des montants.
     *
     * @param ventes liste des ventes
     * @return valeur médiane
     */
    public double calculerMediane(List<Vente> ventes) {
        List<Double> montants = ventes.stream()
                .mapToDouble(Vente::getMontant)
                .sorted()
                .boxed()
                .collect(Collectors.toList());

        int n = montants.size();
        if (n == 0) return 0;
        return (n % 2 == 0)
                ? (montants.get(n / 2 - 1) + montants.get(n / 2)) / 2.0
                : montants.get(n / 2);
    }

    /**
     * Calcule l'écart-type des montants.
     *
     * @param ventes  liste des ventes
     * @param moyenne moyenne déjà calculée
     * @return écart-type
     */
    public double calculerEcartType(List<Vente> ventes, double moyenne) {
        double variance = ventes.stream()
                .mapToDouble(v -> Math.pow(v.getMontant() - moyenne, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    // ─── F5 : Statistiques par catégorie ──────────────────────────────────────

    /**
     * Calcule la somme des montants par catégorie via {@code groupingBy}.
     *
     * @param ventes liste des ventes
     * @return map triée par montant décroissant
     */
    public Map<String, Double> sommesParCategorie(List<Vente> ventes) {
        return ventes.stream()
                .collect(Collectors.groupingBy(
                        Vente::getCategorie,
                        Collectors.summingDouble(Vente::getMontant)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Calcule le nombre de ventes par catégorie.
     *
     * @param ventes liste des ventes
     * @return map catégorie → nombre de ventes
     */
    public Map<String, Long> countParCategorie(List<Vente> ventes) {
        return ventes.stream()
                .collect(Collectors.groupingBy(
                        Vente::getCategorie,
                        Collectors.counting()
                ));
    }

    /**
     * Calcule la moyenne des montants par catégorie.
     *
     * @param ventes liste des ventes
     * @return map catégorie → moyenne
     */
    public Map<String, Double> moyennesParCategorie(List<Vente> ventes) {
        return ventes.stream()
                .collect(Collectors.groupingBy(
                        Vente::getCategorie,
                        Collectors.averagingDouble(Vente::getMontant)
                ));
    }

    /**
     * Calcule les statistiques complètes par catégorie.
     *
     * @param ventes liste des ventes
     * @return map catégorie → StatistiquesResultat
     */
    public Map<String, StatistiquesResultat> statsParCategorie(List<Vente> ventes) {
        return ventes.stream()
                .collect(Collectors.groupingBy(Vente::getCategorie))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> calculerStatistiques(e.getValue())
                ));
    }

    // ─── F5 : Évolution mensuelle ──────────────────────────────────────────────

    /**
     * Calcule la somme des ventes par mois (format "YYYY-MM").
     *
     * @param ventes liste des ventes
     * @return map triée chronologiquement
     */
    public Map<String, Double> sommesParMois(List<Vente> ventes) {
        return ventes.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getDate().getYear() + "-" +
                             String.format("%02d", v.getDate().getMonthValue()),
                        TreeMap::new,
                        Collectors.summingDouble(Vente::getMontant)
                ));
    }

    /**
     * Calcule la somme des ventes par produit.
     *
     * @param ventes liste des ventes
     * @return map produit → somme
     */
    public Map<String, Double> sommesParProduit(List<Vente> ventes) {
        return ventes.stream()
                .collect(Collectors.groupingBy(
                        Vente::getProduit,
                        Collectors.summingDouble(Vente::getMontant)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // ─── F6 : Classement Top-N ────────────────────────────────────────────────

    /**
     * Retourne les N ventes les plus élevées.
     *
     * @param ventes liste des ventes
     * @param n      nombre de ventes à retourner
     * @return liste triée décroissante, limitée à n éléments
     */
    public List<Vente> topNVentes(List<Vente> ventes, int n) {
        return ventes.stream()
                .sorted(Comparator.comparingDouble(Vente::getMontant).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les N produits avec le plus grand chiffre d'affaires.
     *
     * @param ventes liste des ventes
     * @param n      nombre de produits
     * @return map ordonnée produit → CA total
     */
    public Map<String, Double> topNProduits(List<Vente> ventes, int n) {
        return sommesParProduit(ventes).entrySet().stream()
                .limit(n)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // ─── F8 : Filtrage ────────────────────────────────────────────────────────

    /**
     * Filtre les ventes par catégorie (insensible à la casse).
     *
     * @param ventes    liste source
     * @param categorie catégorie cible
     * @return sous-liste filtrée
     */
    public List<Vente> filtrerParCategorie(List<Vente> ventes, String categorie) {
        return ventes.stream()
                .filter(v -> v.getCategorie().equalsIgnoreCase(categorie))
                .collect(Collectors.toList());
    }

    /**
     * Filtre les ventes par période.
     *
     * @param ventes  liste source
     * @param debut   date de début (incluse)
     * @param fin     date de fin (incluse)
     * @return sous-liste filtrée
     */
    public List<Vente> filtrerParPeriode(List<Vente> ventes,
                                         java.time.LocalDate debut,
                                         java.time.LocalDate fin) {
        return ventes.stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());
    }

    /**
     * Filtre par catégorie ET période.
     *
     * @param ventes    liste source
     * @param categorie catégorie cible (null = toutes)
     * @param debut     date de début (null = pas de limite)
     * @param fin       date de fin (null = pas de limite)
     * @return sous-liste filtrée
     */
    public List<Vente> filtrer(List<Vente> ventes,
                               String categorie,
                               java.time.LocalDate debut,
                               java.time.LocalDate fin) {
        return ventes.stream()
                .filter(v -> categorie == null || categorie.isBlank()
                             || v.getCategorie().equalsIgnoreCase(categorie))
                .filter(v -> debut == null || !v.getDate().isBefore(debut))
                .filter(v -> fin   == null || !v.getDate().isAfter(fin))
                .collect(Collectors.toList());
    }

    /**
     * Retourne la liste de toutes les catégories distinctes, triées.
     *
     * @param ventes liste des ventes
     * @return liste triée de catégories
     */
    public List<String> categoriesDistinctes(List<Vente> ventes) {
        return ventes.stream()
                .map(Vente::getCategorie)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Retourne la liste de tous les produits distincts, triés.
     *
     * @param ventes liste des ventes
     * @return liste triée de produits
     */
    public List<String> produitsDistincts(List<Vente> ventes) {
        return ventes.stream()
                .map(Vente::getProduit)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
