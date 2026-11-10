-- ══════════════════════════════════════════════════════════════
-- CSV Analyzer — Schéma de base de données MySQL/MariaDB
-- Auteurs : Rihab Douhab & Soumia El Halla — EMSI 4IIR
-- ══════════════════════════════════════════════════════════════

USE csvanalyzer;

-- ── Table des fichiers importés ──────────────────────────────
CREATE TABLE IF NOT EXISTS fichier_csv (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nom_fichier VARCHAR(255)  NOT NULL,
    chemin      VARCHAR(500),
    nb_lignes   INT           DEFAULT 0,
    nb_erreurs  INT           DEFAULT 0,
    date_import DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_nom (nom_fichier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table des ventes ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vente (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fichier_id  INT            NOT NULL,
    date_vente  DATE           NOT NULL,
    categorie   VARCHAR(100)   NOT NULL,
    produit     VARCHAR(200)   NOT NULL,
    montant     DECIMAL(12,2)  NOT NULL CHECK (montant >= 0),
    date_insert DATETIME       DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_categorie (categorie),
    INDEX idx_date_vente (date_vente),
    INDEX idx_fichier (fichier_id),
    CONSTRAINT fk_vente_fichier FOREIGN KEY (fichier_id)
        REFERENCES fichier_csv(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Vue : statistiques par catégorie ─────────────────────────
CREATE OR REPLACE VIEW v_stats_categorie AS
SELECT
    f.nom_fichier,
    v.categorie,
    COUNT(*)            AS nb_ventes,
    SUM(v.montant)      AS ca_total,
    AVG(v.montant)      AS ca_moyen,
    MIN(v.montant)      AS ca_min,
    MAX(v.montant)      AS ca_max
FROM vente v
JOIN fichier_csv f ON f.id = v.fichier_id
GROUP BY f.nom_fichier, v.categorie
ORDER BY ca_total DESC;

-- ── Vue : évolution mensuelle ────────────────────────────────
CREATE OR REPLACE VIEW v_evolution_mensuelle AS
SELECT
    f.nom_fichier,
    DATE_FORMAT(v.date_vente, '%Y-%m') AS mois,
    COUNT(*)                            AS nb_ventes,
    SUM(v.montant)                      AS ca_total
FROM vente v
JOIN fichier_csv f ON f.id = v.fichier_id
GROUP BY f.nom_fichier, mois
ORDER BY mois;
