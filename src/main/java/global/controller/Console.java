package global.controller;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Maximilian Schirm
 *         15.12.2016
 */
public class Console extends OutputStream {

    private final TextArea textArea;

    public Console(TextArea textArea) {
        this.textArea = textArea;
    }

    public void clearTextArea() {
        textArea.clear();
    }

    @Override
    public void write(int b) throws IOException {
        Platform.runLater(() -> textArea.appendText(String.valueOf((char) b)));
    }
}
