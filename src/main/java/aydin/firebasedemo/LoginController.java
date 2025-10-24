package aydin.firebasedemo;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.QuerySnapshot;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void onSignIn() {
        String email = safe(emailField.getText());
        String password = safe(passwordField.getText());
        if (email.isEmpty() || password.isEmpty()) { setStatus("Email & password required."); return; }

        // Demo-only password check via Firestore (since Admin SDK can't verify password)
        new Thread(() -> {
            try {
                ApiFuture<QuerySnapshot> fut = DemoApp.fstore.collection("users")
                        .whereEqualTo("email", email)
                        .whereEqualTo("password", password)
                        .get();
                boolean ok = !fut.get().isEmpty();
                if (ok) {
                    Platform.runLater(() -> {
                        setStatus("Sign in OK.");
                        try { DemoApp.setRoot("primary"); }
                        catch (IOException ex) { setStatus("Navigate failed: " + ex.getMessage()); }
                    });
                } else {
                    setStatus("Invalid credentials.");
                }
            } catch (InterruptedException | ExecutionException e) {
                setStatus("Sign in error: " + e.getMessage());
            }
        }, "SignInThread").start();
    }

    @FXML
    private void goBack() throws IOException {
        DemoApp.setRoot("welcome");
    }

    private void setStatus(String msg) { Platform.runLater(() -> statusLabel.setText(msg)); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
