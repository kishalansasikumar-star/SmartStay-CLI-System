package smartstay.model;

public class E245549_Guest {
    public final String document, name, phone, email;

    public E245549_Guest(String document, String name, String phone, String email) {
        if (document.isBlank() || name.isBlank()) throw new IllegalArgumentException("Enter both ID and name.");
        if (!phone.matches("[+0-9() -]+") || !phone.replaceAll("[^0-9]", "").matches("[0-9]{7,15}"))
            throw new IllegalArgumentException("Enter a contact number with 7 to 15 digits.");
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            throw new IllegalArgumentException("Enter a valid email address.");
        this.document = document;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }
}
