module uv.naloge.prva.naloga1 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens uv.naloge.prva.naloga1 to javafx.fxml;
    exports uv.naloge.prva.naloga1;
}