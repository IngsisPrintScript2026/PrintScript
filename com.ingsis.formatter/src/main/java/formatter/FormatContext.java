package formatter;

public record FormatContext(
        int indentLevel,
        int indentSpaces,
        boolean spaceBeforeColon,
        boolean spaceAfterColon,
        boolean spaceAroundEquals,
        boolean spaceAroundOperators,
        int lineBreaksAfterPrintln,
        boolean ifBraceSameLine) {

    public FormatContext() {
        this(0, 4, false, true, true, true, 1, true);
    }

    public String getIndent() {
        return " ".repeat(Math.max(0, indentLevel * indentSpaces));
    }

    public FormatContext incrementIndent() {
        return new FormatContext(
                indentLevel + 1,
                indentSpaces,
                spaceBeforeColon,
                spaceAfterColon,
                spaceAroundEquals,
                spaceAroundOperators,
                lineBreaksAfterPrintln,
                ifBraceSameLine);
    }
}
