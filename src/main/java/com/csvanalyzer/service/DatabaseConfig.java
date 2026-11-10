package com.csvanalyzer.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Gestionnaire de connexions MySQL via HikariCP (pool de connexions).
 *
 * <p>Paramètres de connexion configurables via le fichier
 * {@code db.properties} ou les constantes de cette classe.</p>
 *
 * <p>Utilisation du pattern Singleton pour une seule instance
 * du pool par JVM.</p>
 *
 * @author Rihab Douhab &amp; Soumia El Halla
 * @version 1.0
 */
public class DatabaseConfig {

    private static final Logger LOG = Logger.getLogger(DatabaseConfig.class.getName());

    // ─── Paramètres de connexion ──────────────────────────────────────────────
    private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/csvanalyzer"
            + "?useUnicode=true&characterEncoding=UTF-8"
            + "&serverTimezone=Africa/Casablanca"
            + "&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER   = "root";
    private static final String DB_PASS   = "";          // mot de passe XAMPP root vide par défaut

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static HikariDataSource dataSource;

    private DatabaseConfig() {}

    /**
     * Retourne une connexion depuis le pool HikariCP.
     * Initialise le pool au premier appel.
     *
     * @return connexion JDBC prête à l'emploi
     * @throws SQLException si la connexion échoue
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialiserPool();
        }
        return dataSource.getConnection();
    }

    /**
     * Configure et démarre le pool de connexions HikariCP.
     */
    private static void initialiserPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // ── Pool settings ──
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);     // 30 secondes
        config.setIdleTimeout(600_000);           // 10 minutes
        config.setMaxLifetime(1_800_000);         // 30 minutes
        config.setPoolName("CSVAnalyzer-Pool");

        // ── Validation ──
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
        LOG.info("Pool HikariCP initialisé — URL : " + JDBC_URL);
    }

    /**
     * Ferme le pool de connexions (à appeler à la fermeture de l'application).
     */
    public static synchronized void fermer() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOG.info("Pool HikariCP fermé.");
        }
    }

    /**
     * Teste si la connexion à la base de données est possible.
     *
     * @return true si la connexion réussit
     */
    public static boolean testerConnexion() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOG.warning("Connexion DB impossible : " + e.getMessage());
            return false;
        }
    }
}
