package aydin.firebasedemo;

import javafx.fxml.FXML;
import java.io.IOException;

public class WelcomeController {

    @FXML
    private void goLogin() throws IOException {
        DemoApp.setRoot("login");
    }

    @FXML
    private void goRegister() throws IOException {
        DemoApp.setRoot("register");
    }
}
