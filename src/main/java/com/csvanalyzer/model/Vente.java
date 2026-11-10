package com.csvanalyzer.model;

import java.time.LocalDate;

/**
 * Modèle représentant une ligne de vente du fichier CSV.
 * Structure : date, categorie, produit, montant
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class Vente {

    /** Date de la transaction */
    private LocalDate date;

    /** Catégorie du produit */
    private String categorie;

    /** Nom du produit */
    private String produit;

    /** Montant de la vente */
    private double montant;

    /**
     * Constructeur complet.
     *
     * @param date      date de la vente
     * @param categorie catégorie du produit
     * @param produit   nom du produit
     * @param montant   montant en MAD
     */
    public Vente(LocalDate date, String categorie, String produit, double montant) {
        this.date = date;
        this.categorie = categorie;
        this.produit = produit;
        this.montant = montant;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /** @return date de la vente */
    public LocalDate getDate() { return date; }

    /** @return catégorie du produit */
    public String getCategorie() { return categorie; }

    /** @return nom du produit */
    public String getProduit() { return produit; }

    /** @return montant de la vente */
    public double getMontant() { return montant; }

    // ─── Setters ──────────────────────────────────────────────────────────────

    /** @param date nouvelle date */
    public void setDate(LocalDate date) { this.date = date; }

    /** @param categorie nouvelle catégorie */
    public void setCategorie(String categorie) { this.categorie = categorie; }

    /** @param produit nouveau produit */
    public void setProduit(String produit) { this.produit = produit; }

    /** @param montant nouveau montant */
    public void setMontant(double montant) { this.montant = montant; }

    @Override
    public String toString() {
        return String.format("Vente{date=%s, categorie='%s', produit='%s', montant=%.2f}",
                date, categorie, produit, montant);
    }
}
