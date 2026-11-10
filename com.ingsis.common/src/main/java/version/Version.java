/*
 * My Project
 */

package version;

public enum Version {
    V_1_0("1.0"),
    V_1_1("1.1"),
    V_1_2("1.2");

    private final String label;

    Version(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Version fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Version string cannot be null");
        }

        return switch (value.trim()) {
            case "1.0" -> V_1_0;
            case "1.1" -> V_1_1;
            case "1.2" -> V_1_2;
            default -> throw new IllegalArgumentException("Unknown version: " + value);
        };
    }
}
