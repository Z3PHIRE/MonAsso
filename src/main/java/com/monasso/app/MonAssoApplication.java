package com.monasso.app;

import com.monasso.app.config.AppContext;
import com.monasso.app.config.AppInitializer;
import com.monasso.app.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonAssoApplication extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonAssoApplication.class);

    private AppContext appContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            appContext = new AppInitializer().initialize();
            MainView mainView = new MainView(appContext);
            Scene scene = new Scene(mainView.getRoot(), 1280, 780);
            appContext.themeManager().attach(scene);

            stage.setScene(scene);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle(appContext.brandingService().getCurrentBranding().appName());
            setStageIcon(stage);
            appContext.brandingService().brandingProperty().addListener((obs, oldConfig, newConfig) -> {
                stage.setTitle(newConfig.appName());
                setStageIcon(stage);
            });
            stage.show();
        } catch (Exception e) {
            LOGGER.error("Echec de demarrage de l'application", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de demarrage");
            alert.setHeaderText("MonAsso n'a pas pu demarrer");
            alert.setContentText(buildStartupErrorMessage(e));
            alert.showAndWait();
        }
    }

    @Override
    public void stop() {
        if (appContext != null) {
            appContext.close();
        }
    }

    private void setStageIcon(Stage stage) {
        Image icon = appContext.brandingService().loadAppIcon();
        stage.getIcons().setAll(icon);
    }

    private String buildStartupErrorMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "Une erreur technique est survenue au demarrage. Consultez les logs pour plus de details.";
        }
        return message;
    }
}
