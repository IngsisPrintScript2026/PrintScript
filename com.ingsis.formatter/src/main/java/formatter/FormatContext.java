/*
 * My Project
 */

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
        Boolean ifBraceBelowLine,
        Boolean spaceAfterComma) {

    public FormatContext(
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
        this(
                indentLevel,
                indentSpaces,
                spaceBeforeColon,
                spaceAfterColon,
                spaceAroundEquals,
                spaceAroundOperators,
                lineBreakAfterStatement,
                lineBreaksAfterPrintln,
                singleSpaceSeparation,
                ifBraceSameLine,
                ifBraceBelowLine,
                true);
    }

    public FormatContext() {
        this(0, 4, false, true, true, true, false, 1, false, true, false, true);
    }

    public boolean isSpaceBeforeColon() {
        return Boolean.TRUE.equals(spaceBeforeColon);
    }

    public boolean isSpaceAfterColon() {
        return spaceAfterColon != null ? spaceAfterColon : true;
    }

    public boolean isSpaceAroundEquals() {
        return spaceAroundEquals != null ? spaceAroundEquals : true;
    }

    public boolean isSpaceAroundOperators() {
        return spaceAroundOperators != null ? spaceAroundOperators : true;
    }

    public boolean isIfBraceSameLine() {
        return ifBraceSameLine != null ? ifBraceSameLine : true;
    }

    public boolean isSpaceAfterComma() {
        return spaceAfterComma != null ? spaceAfterComma : true;
    }

    public int getIndentSpaces() {
        return indentSpaces != null ? indentSpaces : 4;
    }

    public int getLineBreaksAfterPrintln() {
        return lineBreaksAfterPrintln != null ? lineBreaksAfterPrintln : 1;
    }

    public String getIndent() {
        return " ".repeat(Math.max(0, indentLevel * getIndentSpaces()));
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
                ifBraceBelowLine,
                spaceAfterComma);
    }
}
