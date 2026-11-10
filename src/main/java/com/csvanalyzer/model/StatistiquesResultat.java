package com.csvanalyzer.model;

/**
 * Statistiques descriptives calculées sur une liste de ventes.
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class StatistiquesResultat {

    private final long   nombre;
    private final double somme;
    private final double moyenne;
    private final double minimum;
    private final double maximum;
    private final double mediane;
    private final double ecartType;

    /**
     * Constructeur complet.
     */
    public StatistiquesResultat(long nombre, double somme, double moyenne,
                                double minimum, double maximum,
                                double mediane, double ecartType) {
        this.nombre    = nombre;
        this.somme     = somme;
        this.moyenne   = moyenne;
        this.minimum   = minimum;
        this.maximum   = maximum;
        this.mediane   = mediane;
        this.ecartType = ecartType;
    }

    public long   getNombre()    { return nombre;    }
    public double getSomme()     { return somme;     }
    public double getMoyenne()   { return moyenne;   }
    public double getMinimum()   { return minimum;   }
    public double getMaximum()   { return maximum;   }
    public double getMediane()   { return mediane;   }
    public double getEcartType() { return ecartType; }

    @Override
    public String toString() {
        return String.format(
            "Stats{n=%d, somme=%.2f, moy=%.2f, min=%.2f, max=%.2f, med=%.2f, σ=%.2f}",
            nombre, somme, moyenne, minimum, maximum, mediane, ecartType);
    }
}
