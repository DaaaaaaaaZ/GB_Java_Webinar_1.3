package dz.webinar3.lesson2.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Controller {
    private Client client;

    @FXML
    private TextField textField;

    @FXML
    private TextArea textArea;

    public synchronized void showMessage (String message) {
        if (message.isEmpty() || message.isBlank()) {
            return;
        } else {
            textArea.appendText(message + "\n");
        }
    }

    public void onClickButton(ActionEvent event) {
        String message = textField.getText();
        if (!message.isEmpty() && !message.isBlank()) {
            client.sendMessage(message);
        }
        textField.setText("");
        textField.requestFocus();
    }

    public void setClient(Client client) {
        this.client = client;
    }
}