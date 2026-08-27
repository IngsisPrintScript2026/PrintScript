package formatter;

public record FormatContext(
        int indentLevel,
        Integer indentSpaces,
        Boolean spaceBeforeColon,
        Boolean spaceAfterColon,
        Boolean spaceAroundEquals,
        Boolean spaceAroundOperators,
        Boolean lineBreakAfterStatement,
        Integer lineBreaksAfterPrintln,
        Boolean singleSpaceSeparation,
        Boolean ifBraceSameLine,
        Boolean ifBraceBelowLine) {

    public FormatContext() {
        this(0, null, null, null, null, null, null, null, null, null, null);
    }

    public String getIndent() {
        int spaces = (indentSpaces != null) ? indentSpaces : 4;
        return " ".repeat(Math.max(0, indentLevel * spaces));
    }

    public FormatContext incrementIndent() {
        return new FormatContext(
                indentLevel + 1,
                indentSpaces,
                spaceBeforeColon,
                spaceAfterColon,
                spaceAroundEquals,
                spaceAroundOperators,
                lineBreakAfterStatement,
                lineBreaksAfterPrintln,
                singleSpaceSeparation,
                ifBraceSameLine,
                ifBraceBelowLine);
    }
}
