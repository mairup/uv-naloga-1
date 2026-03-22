module uv.naloge.prvaNaloga {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;

    requires org.kordamp.bootstrapfx.core;

    opens uv.naloge.prvaNaloga to javafx.fxml;
    exports uv.naloge.prvaNaloga;
}