package uv.naloge.prvaNaloga;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class AssignmentOneApplication extends Application {
    private static final String BUNDLE_BASE_NAME = "uv.naloge.prvaNaloga.messages";
    private static final String PREFERRED_LANGUAGE_KEY = "preferredLanguage";
    private static final String DEFAULT_LANGUAGE = "sl";
    private static final Preferences USER_PREFERENCES = Preferences.userNodeForPackage(AssignmentOneApplication.class);

    private static Stage primaryStage;
    private static HostServices hostServices;
    private static Locale currentLocale = resolveInitialLocale();

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        hostServices = getHostServices();
        reloadScene();
        stage.show();
    }

    public static void switchLanguage(Locale targetLocale) {
        if (targetLocale == null || targetLocale.equals(currentLocale)) {
            return;
        }

        currentLocale = targetLocale;
        USER_PREFERENCES.put(PREFERRED_LANGUAGE_KEY, targetLocale.getLanguage());
        try {
            reloadScene();
        } catch (IOException exception) {
            throw new RuntimeException("Could not reload scene for selected language.", exception);
        }
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void openExternalLink(String url) {
        if (hostServices != null) {
            hostServices.showDocument(url);
        }
    }

    private static Locale resolveInitialLocale() {
        String storedLanguage = USER_PREFERENCES.get(PREFERRED_LANGUAGE_KEY, DEFAULT_LANGUAGE);
        return Locale.forLanguageTag(storedLanguage);
    }

    private static void reloadScene() throws IOException {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
        FXMLLoader fxmlLoader = new FXMLLoader(
                AssignmentOneApplication.class.getResource("main-view.fxml"),
                resourceBundle
        );

        Parent rootNode = fxmlLoader.load();
        Scene scene = new Scene(rootNode, 850, 550);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        scene.getStylesheets().add(Objects.requireNonNull(AssignmentOneApplication.class.getResource("styles.css")).toExternalForm());

        primaryStage.setTitle(resourceBundle.getString("app.title"));
        primaryStage.setScene(scene);
    }
}
