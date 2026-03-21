module uv.naloge.prvaNaloga {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens uv.naloge.prvaNaloga to javafx.fxml;
    exports uv.naloge.prvaNaloga;
}