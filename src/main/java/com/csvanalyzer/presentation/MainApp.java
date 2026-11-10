package com.csvanalyzer.presentation;

import com.csvanalyzer.service.DatabaseConfig;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application CSV Analyzer.
 *
 * <p>Lance l'interface JavaFX et délègue la construction de la scène
 * au {@link MainController}. Ferme le pool de connexions DB à la sortie.</p>
 *
 * <p><b>Auteurs :</b> Rihab Douhab &amp; Soumia El Halla — EMSI Moulay Youssef, 4IIR</p>
 *
 * @version 1.0
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController();
        controller.demarrer(stage);
    }

    /**
     * Appelé par JavaFX à la fermeture — libère le pool HikariCP.
     */
    @Override
    public void stop() {
        DatabaseConfig.fermer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
