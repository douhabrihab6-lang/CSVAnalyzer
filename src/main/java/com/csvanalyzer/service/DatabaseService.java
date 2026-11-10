package com.csvanalyzer.service;

import com.csvanalyzer.model.StatistiquesResultat;
import com.csvanalyzer.model.Vente;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service d'accès à la base de données MySQL/MariaDB.
 *
 * <p>Opérations disponibles :</p>
 * <ul>
 *   <li>Sauvegarder un import CSV (fichier + ventes) en base</li>
 *   <li>Charger les ventes depuis la base</li>
 *   <li>Lister les fichiers importés</li>
 *   <li>Supprimer un import</li>
 *   <li>Lire les statistiques directement depuis les vues SQL</li>
 * </ul>
 *
 * <p>Toutes les requêtes utilisent des {@code PreparedStatement}
 * pour prévenir les injections SQL.</p>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class DatabaseService {

    private static final Logger LOG = Logger.getLogger(DatabaseService.class.getName());

    // ─── Insertion d'un fichier CSV en base ───────────────────────────────────

    /**
     * Sauvegarde un fichier CSV importé et toutes ses ventes en base de données.
     *
     * <p>Utilise une transaction : si une vente échoue, tout est annulé.</p>
     *
     * @param nomFichier  nom du fichier CSV
     * @param chemin      chemin absolu du fichier
     * @param ventes      liste des ventes à insérer
     * @param nbErreurs   nombre de lignes ignorées lors de la lecture
     * @return id du fichier créé en base
     * @throws SQLException en cas d'erreur SQL
     */
    public int sauvegarderImport(String nomFichier, String chemin,
                                  List<Vente> ventes, int nbErreurs) throws SQLException {
        int fichierId;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);  // début de transaction

            try {
                // 1. Insérer ou mettre à jour l'enregistrement fichier
                fichierId = insererOuMettreAJourFichier(conn, nomFichier, chemin,
                        ventes.size(), nbErreurs);

                // 2. Supprimer les anciennes ventes de ce fichier (ré-import)
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM vente WHERE fichier_id = ?")) {
                    ps.setInt(1, fichierId);
                    ps.executeUpdate();
                }

                // 3. Insérer toutes les ventes en batch
                insererVentesBatch(conn, fichierId, ventes);

                conn.commit();
                LOG.info(String.format("Import sauvegardé : fichier_id=%d, %d ventes",
                        fichierId, ventes.size()));

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return fichierId;
    }

    /**
     * Insère ou met à jour l'entrée dans {@code fichier_csv}.
     */
    private int insererOuMettreAJourFichier(Connection conn, String nomFichier,
                                             String chemin, int nbLignes,
                                             int nbErreurs) throws SQLException {
        // Vérifier si le fichier existe déjà
        String sqlSelect = "SELECT id FROM fichier_csv WHERE nom_fichier = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, nomFichier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    // Mettre à jour
                    String sqlUpdate = "UPDATE fichier_csv SET chemin=?, nb_lignes=?, " +
                                       "nb_erreurs=?, date_import=NOW() WHERE id=?";
                    try (PreparedStatement pu = conn.prepareStatement(sqlUpdate)) {
                        pu.setString(1, chemin);
                        pu.setInt(2, nbLignes);
                        pu.setInt(3, nbErreurs);
                        pu.setInt(4, id);
                        pu.executeUpdate();
                    }
                    return id;
                }
            }
        }

        // Insérer nouveau
        String sqlInsert = "INSERT INTO fichier_csv (nom_fichier, chemin, nb_lignes, nb_erreurs) " +
                           "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nomFichier);
            ps.setString(2, chemin);
            ps.setInt(3, nbLignes);
            ps.setInt(4, nbErreurs);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'ID du fichier inséré.");
    }

    /**
     * Insère les ventes en mode batch (performances optimales pour gros fichiers).
     */
    private void insererVentesBatch(Connection conn, int fichierId,
                                    List<Vente> ventes) throws SQLException {
        String sql = "INSERT INTO vente (fichier_id, date_vente, categorie, produit, montant) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int batchCount = 0;
            for (Vente v : ventes) {
                ps.setInt(1, fichierId);
                ps.setDate(2, java.sql.Date.valueOf(v.getDate()));
                ps.setString(3, v.getCategorie());
                ps.setString(4, v.getProduit());
                ps.setDouble(5, v.getMontant());
                ps.addBatch();

                // Exécuter par lots de 500 pour éviter de saturer la mémoire
                if (++batchCount % 500 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch(); // dernier lot
        }
    }

    // ─── Lecture des ventes ───────────────────────────────────────────────────

    /**
     * Charge toutes les ventes d'un fichier depuis la base de données.
     *
     * @param fichierId identifiant du fichier
     * @return liste des ventes
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Vente> chargerVentes(int fichierId) throws SQLException {
        List<Vente> ventes = new ArrayList<>();
        String sql = "SELECT date_vente, categorie, produit, montant " +
                     "FROM vente WHERE fichier_id = ? ORDER BY date_vente";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fichierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ventes.add(new Vente(
                            rs.getDate("date_vente").toLocalDate(),
                            rs.getString("categorie"),
                            rs.getString("produit"),
                            rs.getDouble("montant")
                    ));
                }
            }
        }
        return ventes;
    }

    /**
     * Charge les ventes d'un fichier en appliquant des filtres optionnels.
     *
     * @param fichierId  identifiant du fichier
     * @param categorie  filtre catégorie (null = toutes)
     * @param debut      date de début (null = pas de limite)
     * @param fin        date de fin (null = pas de limite)
     * @return liste filtrée
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Vente> chargerVentesFiltrees(int fichierId, String categorie,
                                              LocalDate debut, LocalDate fin) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT date_vente, categorie, produit, montant FROM vente WHERE fichier_id = ?");

        List<Object> params = new ArrayList<>();
        params.add(fichierId);

        if (categorie != null && !categorie.isBlank()) {
            sql.append(" AND LOWER(categorie) = LOWER(?)");
            params.add(categorie);
        }
        if (debut != null) {
            sql.append(" AND date_vente >= ?");
            params.add(java.sql.Date.valueOf(debut));
        }
        if (fin != null) {
            sql.append(" AND date_vente <= ?");
            params.add(java.sql.Date.valueOf(fin));
        }
        sql.append(" ORDER BY date_vente");

        List<Vente> ventes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ventes.add(new Vente(
                            rs.getDate("date_vente").toLocalDate(),
                            rs.getString("categorie"),
                            rs.getString("produit"),
                            rs.getDouble("montant")
                    ));
                }
            }
        }
        return ventes;
    }

    // ─── Gestion des fichiers importés ────────────────────────────────────────

    /**
     * Retourne la liste de tous les fichiers CSV importés en base.
     *
     * @return liste de maps avec les colonnes : id, nom_fichier, nb_lignes, nb_erreurs, date_import
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Map<String, Object>> listerFichiers() throws SQLException {
        List<Map<String, Object>> fichiers = new ArrayList<>();
        String sql = "SELECT id, nom_fichier, nb_lignes, nb_erreurs, date_import " +
                     "FROM fichier_csv ORDER BY date_import DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",           rs.getInt("id"));
                row.put("nom_fichier",  rs.getString("nom_fichier"));
                row.put("nb_lignes",    rs.getInt("nb_lignes"));
                row.put("nb_erreurs",   rs.getInt("nb_erreurs"));
                row.put("date_import",  rs.getTimestamp("date_import").toString());
                fichiers.add(row);
            }
        }
        return fichiers;
    }

    /**
     * Supprime un fichier importé et toutes ses ventes (CASCADE).
     *
     * @param fichierId identifiant du fichier à supprimer
     * @throws SQLException en cas d'erreur SQL
     */
    public void supprimerImport(int fichierId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM fichier_csv WHERE id = ?")) {
            ps.setInt(1, fichierId);
            int rows = ps.executeUpdate();
            LOG.info("Import supprimé : fichier_id=" + fichierId + ", rows=" + rows);
        }
    }

    // ─── Statistiques SQL ─────────────────────────────────────────────────────

    /**
     * Calcule les statistiques globales directement en SQL (très rapide).
     *
     * @param fichierId identifiant du fichier
     * @return {@link StatistiquesResultat}
     * @throws SQLException en cas d'erreur SQL
     */
    public StatistiquesResultat calculerStatistiquesSQL(int fichierId) throws SQLException {
        String sql = "SELECT COUNT(*) AS n, SUM(montant) AS somme, " +
                     "AVG(montant) AS moyenne, MIN(montant) AS minimum, " +
                     "MAX(montant) AS maximum " +
                     "FROM vente WHERE fichier_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fichierId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long   n       = rs.getLong("n");
                    double somme   = rs.getDouble("somme");
                    double moyenne = rs.getDouble("moyenne");
                    double min     = rs.getDouble("minimum");
                    double max     = rs.getDouble("maximum");
                    // Médiane et écart-type calculés côté Java (SQL standard non supporté partout)
                    return new StatistiquesResultat(n, somme, moyenne, min, max, 0, 0);
                }
            }
        }
        return new StatistiquesResultat(0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Retourne le CA par catégorie depuis la vue SQL {@code v_stats_categorie}.
     *
     * @param nomFichier nom du fichier
     * @return map catégorie → CA total
     * @throws SQLException en cas d'erreur SQL
     */
    public Map<String, Double> caParCategorieSQL(String nomFichier) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT categorie, ca_total FROM v_stats_categorie " +
                     "WHERE nom_fichier = ? ORDER BY ca_total DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomFichier);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("categorie"), rs.getDouble("ca_total"));
                }
            }
        }
        return result;
    }

    /**
     * Retourne l'évolution mensuelle depuis la vue SQL {@code v_evolution_mensuelle}.
     *
     * @param nomFichier nom du fichier
     * @return map mois → CA total (trié chronologiquement)
     * @throws SQLException en cas d'erreur SQL
     */
    public Map<String, Double> evolutionMensuelleSQL(String nomFichier) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT mois, ca_total FROM v_evolution_mensuelle " +
                     "WHERE nom_fichier = ? ORDER BY mois";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomFichier);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("mois"), rs.getDouble("ca_total"));
                }
            }
        }
        return result;
    }

    /**
     * Retourne le Top-N produits par CA.
     *
     * @param fichierId identifiant du fichier
     * @param n         nombre de produits à retourner
     * @return map produit → CA
     * @throws SQLException en cas d'erreur SQL
     */
    public Map<String, Double> topNProduitsSQL(int fichierId, int n) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT produit, SUM(montant) AS ca " +
                     "FROM vente WHERE fichier_id = ? " +
                     "GROUP BY produit ORDER BY ca DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fichierId);
            ps.setInt(2, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("produit"), rs.getDouble("ca"));
                }
            }
        }
        return result;
    }
}
