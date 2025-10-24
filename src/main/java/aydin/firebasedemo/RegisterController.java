package aydin.firebasedemo;

import com.google.cloud.firestore.DocumentReference;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void onRegister() {
        String email = safe(emailField.getText());
        String password = safe(passwordField.getText());
        if (email.isEmpty() || password.isEmpty()) { setStatus("Email & password required."); return; }

        // 1) Create Firebase Auth user via Admin SDK
        final String uid;
        try {
            UserRecord.CreateRequest req = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(email.split("@")[0])
                    .setDisabled(false);
            UserRecord user = DemoApp.fauth.createUser(req);
            uid = user.getUid();
        } catch (FirebaseAuthException e) {
            setStatus("Register failed: " + e.getMessage());
            return;
        }

        // 2) Store creds in Firestore (DEMO only; not secure for production)
        try {
            DocumentReference ref = DemoApp.fstore.collection("users").document(uid);
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("password", password);
            ref.set(data);

            setStatus("Account created. Go to login.");
        } catch (Exception e) {
            setStatus("Saved in Auth, but Firestore write failed: " + e.getMessage());
        }
    }

    @FXML
    private void goLogin() throws IOException {
        DemoApp.setRoot("login"); // per your choice: after register → go to login
    }

    private void setStatus(String msg) { Platform.runLater(() -> statusLabel.setText(msg)); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
