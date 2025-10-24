package aydin.firebasedemo;

import javafx.beans.property.*;

public class Person {

    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty age = new SimpleIntegerProperty();
    private final StringProperty phone = new SimpleStringProperty();

    public Person() { } // needed for Firestore POJO mapping (if used)

    public Person(String name, int age, String phone) {
        this.name.set(name);
        this.age.set(age);
        this.phone.set(phone);
    }

    // name
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    // age
    public int getAge() { return age.get(); }
    public void setAge(int v) { age.set(v); }
    public IntegerProperty ageProperty() { return age; }

    // phone
    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(v); }
    public StringProperty phoneProperty() { return phone; }

    @Override
    public String toString() {
        return name.get() + " (" + age.get() + ") - " + (phone.get() == null ? "" : phone.get());
    }
}
