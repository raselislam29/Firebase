package aydin.firebasedemo;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PrimaryController {

    @FXML private TextField ageTextField;
    @FXML private TextField nameTextField;
    @FXML private TextField phoneTextField; // NEW: phone input field

    @FXML private TextArea outputTextArea;

    @FXML private Button readButton;
    @FXML private Button registerButton;
    @FXML private Button switchSecondaryViewButton;
    @FXML private Button writeButton;

    private boolean key;
    private final ObservableList<Person> listOfUsers = FXCollections.observableArrayList();
    private Person person;

    public ObservableList<Person> getListOfUsers() {
        return listOfUsers;
    }

    // Let FXML call this automatically
    @FXML
    private void initialize() {
        AccessDataView accessDataViewModel = new AccessDataView();
        nameTextField.textProperty().bindBidirectional(accessDataViewModel.personNameProperty());
        // Using your current property name from AccessDataView:
        writeButton.disableProperty().bind(accessDataViewModel.isWritePossibleProperty().not());
    }

    // -------------------
    // Button handlers
    // -------------------

    @FXML
    private void readButtonClicked(ActionEvent event) {
        // Use async version to avoid freezing UI
        readFirebaseAsync();
    }

    @FXML
    private void registerButtonClicked(ActionEvent event) {
        boolean ok = registerUser();
        if (ok) {
            info("Success", "User registered.\nCheck Firebase → Authentication → Users.");
        } else {
            warn("Register failed", "See run console for details.");
        }
    }

    @FXML
    private void writeButtonClicked(ActionEvent event) {
        addData();
    }

    @FXML
    private void switchToSecondary() throws IOException {
        DemoApp.setRoot("secondary");
    }

    // -------------------
    // ORIGINAL read (blocking) - kept, but enhanced to show Phone if present
    // -------------------
    public boolean readFirebase() {
        key = false;

        ApiFuture<QuerySnapshot> future = DemoApp.fstore.collection("Persons").get();
        List<QueryDocumentSnapshot> documents;
        try {
            documents = future.get().getDocuments();
            if (documents.size() > 0) {
                System.out.println("Getting (reading) data from firebase database....");
                listOfUsers.clear();
                outputTextArea.clear();
                for (QueryDocumentSnapshot document : documents) {
                    Object n = document.getData().get("Name");
                    Object a = document.getData().get("Age");
                    Object p = document.getData().get("Phone"); // NEW: phone
                    String name = n == null ? "" : String.valueOf(n);
                    String ageStr = a == null ? "0" : String.valueOf(a);
                    String phone = p == null ? "" : String.valueOf(p);

                    outputTextArea.appendText(
                            name + " , Age: " + ageStr + " , Phone: " + phone + " \n "
                    );

                    System.out.println(document.getId() + " => " + name);
                    try {
                        int age = Integer.parseInt(ageStr);
                        person = new Person(name, age, phone); // Person now supports phone
                        listOfUsers.add(person);
                    } catch (NumberFormatException ignore) {
                        // Skip invalid age rows safely
                    }
                }
            } else {
                System.out.println("No data");
                outputTextArea.appendText("No data.\n");
            }
            key = true;

        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
        return key;
    }

    // -------------------
    // NEW: Non-blocking read using a background Task
    // -------------------
    private void readFirebaseAsync() {
        outputTextArea.appendText("Reading from Firestore...\n");

        Task<List<Person>> task = new Task<>() {
            @Override
            protected List<Person> call() throws Exception {
                ApiFuture<QuerySnapshot> future = DemoApp.fstore.collection("Persons").get();
                QuerySnapshot snap = future.get();
                List<Person> people = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap.getDocuments()) {
                    String name = String.valueOf(doc.get("Name"));
                    String ageStr = String.valueOf(doc.get("Age"));
                    String phone = doc.contains("Phone") && doc.get("Phone") != null
                            ? String.valueOf(doc.get("Phone"))
                            : "";
                    try {
                        int age = Integer.parseInt(ageStr);
                        people.add(new Person(name, age, phone));
                    } catch (NumberFormatException ignore) {
                        // Skip invalid entries
                    }
                }
                return people;
            }
        };

        task.setOnSucceeded(e -> {
            List<Person> people = task.getValue();
            listOfUsers.setAll(people);
            outputTextArea.clear();
            if (people.isEmpty()) {
                outputTextArea.appendText("No data.\n");
            } else {
                for (Person p : people) {
                    outputTextArea.appendText(
                            p.getName() + " , Age: " + p.getAge() + " , Phone: " + p.getPhone() + "\n"
                    );
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            error("Read failed", ex != null ? ex.getMessage() : "Unknown error");
        });

        new Thread(task, "FirestoreRead").start();
    }

    // -------------------
    // Auth (unchanged logic from your original, just wrapped with UI feedback above)
    // -------------------
    public boolean registerUser() {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail("user222@example.com")
                .setEmailVerified(false)
                .setPassword("secretPassword")
                .setPhoneNumber("+11234567890")
                .setDisplayName("John Doe")
                .setDisabled(false);

        UserRecord userRecord;
        try {
            userRecord = DemoApp.fauth.createUser(request);
            System.out.println("Successfully created new user with Firebase Uid: " + userRecord.getUid()
                    + " check Firebase > Authentication > Users tab");
            return true;

        } catch (FirebaseAuthException ex) {
            System.out.println("Error creating a new user in the firebase");
            return false;
        }
    }

    // -------------------
    // Write (UPDATED: now saves Phone)
    // -------------------
    public void addData() {
        String name = nameTextField.getText() == null ? "" : nameTextField.getText().trim();
        String ageStr = ageTextField.getText() == null ? "" : ageTextField.getText().trim();
        String phone = phoneTextField.getText() == null ? "" : phoneTextField.getText().trim();

        if (name.isEmpty()) {
            warn("Validation", "Name cannot be empty.");
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 0) throw new NumberFormatException("negative");
        } catch (NumberFormatException ex) {
            warn("Validation", "Age must be a non-negative integer.");
            return;
        }
        if (phone.isEmpty()) {
            warn("Validation", "Phone cannot be empty.");
            return;
        }

        DocumentReference docRef = DemoApp.fstore.collection("Persons")
                .document(UUID.randomUUID().toString());

        Map<String, Object> data = new HashMap<>();
        data.put("Name", name);
        data.put("Age", age);
        data.put("Phone", phone); // NEW: phone saved

        ApiFuture<WriteResult> result = docRef.set(data);

        // Confirm write on a background thread, then notify UI
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                result.get();
                return null;
            }
        };
        task.setOnSucceeded(e -> info("Write OK", "Saved to Firestore."));
        task.setOnFailed(e -> error("Write failed",
                task.getException() != null ? task.getException().getMessage() : "Unknown error"));
        new Thread(task, "FirestoreWrite").start();
    }

    // -------------------
    // Small alert helpers
    // -------------------
    private void info(String title, String msg) { show(Alert.AlertType.INFORMATION, title, msg); }
    private void warn(String title, String msg) { show(Alert.AlertType.WARNING, title, msg); }
    private void error(String title, String msg) { show(Alert.AlertType.ERROR, title, msg); }

    private void show(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.show();
        });
    }
}
