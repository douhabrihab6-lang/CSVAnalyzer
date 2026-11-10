package com.csvanalyzer.model;

/**
 * Résultat de la lecture d'un fichier CSV.
 * Contient les ventes valides et le nombre de lignes ignorées.
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class LectureResultat {

    private final java.util.List<Vente> ventes;
    private final int lignesIgnorees;
    private final java.util.List<String> erreursMessages;

    /**
     * @param ventes          liste des ventes correctement parsées
     * @param lignesIgnorees  nombre de lignes invalides
     * @param erreursMessages détail des erreurs par ligne
     */
    public LectureResultat(java.util.List<Vente> ventes,
                           int lignesIgnorees,
                           java.util.List<String> erreursMessages) {
        this.ventes = ventes;
        this.lignesIgnorees = lignesIgnorees;
        this.erreursMessages = erreursMessages;
    }

    /** @return liste des ventes valides */
    public java.util.List<Vente> getVentes() { return ventes; }

    /** @return nombre de lignes ignorées */
    public int getLignesIgnorees() { return lignesIgnorees; }

    /** @return messages d'erreur */
    public java.util.List<String> getErreursMessages() { return erreursMessages; }

    /** @return true si aucune erreur */
    public boolean isSuccess() { return lignesIgnorees == 0; }
}
