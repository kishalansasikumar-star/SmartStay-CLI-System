package smartstay.model;

public class E245549_Guest {
    public final String document, name, phone, email;

    public E245549_Guest(String document, String name, String phone, String email) {
        this.document = document;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public void validateForRegistration() {
        if (document.codePoints().noneMatch(Character::isLetterOrDigit))
            throw new IllegalArgumentException("NIC/passport must contain at least one letter or digit.");
        if (name.isBlank()) throw new IllegalArgumentException("Enter the guest's full name.");

        String digits = phone.replaceAll("[^0-9]", "");
        String group = "(?:[0-9]+|\\([0-9]+\\))";
        if (digits.length() < 7 || digits.length() > 15
                || !phone.matches("\\+?" + group + "(?:[ .-]?" + group + ")*"))
            throw new IllegalArgumentException("Enter a phone number with 7 to 15 digits. Use + only at the start, for example +94 77 123 4567.");

        String localPart = "[\\p{L}\\p{M}\\p{N}!#$%&'*+/=?^_\u0060{|}~-]+";
        String domainLabel = "[\\p{L}\\p{N}](?:[\\p{L}\\p{M}\\p{N}-]{0,61}[\\p{L}\\p{M}\\p{N}])?";
        String address = localPart + "(?:\\." + localPart + ")*@" + domainLabel + "(?:\\." + domainLabel + ")+";
        if (email.length() > 254 || !email.matches(address))
            throw new IllegalArgumentException("Enter an email like name@example.com. Check the @ sign and the text between dots.");
    }
}
