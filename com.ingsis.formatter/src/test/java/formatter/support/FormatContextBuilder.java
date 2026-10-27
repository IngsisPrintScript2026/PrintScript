/*
 * My Project
 */

package formatter.support;

import formatter.FormatContext;

/** Fluent builder for {@link FormatContext}: every option starts unset (null). */
public final class FormatContextBuilder {
    private Integer indentSpaces;
    private Boolean spaceBeforeColon;
    private Boolean spaceAfterColon;
    private Boolean spaceAroundEquals;
    private Integer lineBreaksAfterPrintln;
    private Boolean ifBraceSameLine;
    private Boolean ifBraceBelowLine;
    private Boolean spaceAfterComma;
    private Integer lineBreaksBeforePrintln;

    public static FormatContextBuilder unset() {
        return new FormatContextBuilder();
    }

    public FormatContextBuilder indentSpaces(Integer value) {
        this.indentSpaces = value;
        return this;
    }

    public FormatContextBuilder spaceBeforeColon(Boolean value) {
        this.spaceBeforeColon = value;
        return this;
    }

    public FormatContextBuilder spaceAfterColon(Boolean value) {
        this.spaceAfterColon = value;
        return this;
    }

    public FormatContextBuilder spaceAroundEquals(Boolean value) {
        this.spaceAroundEquals = value;
        return this;
    }

    public FormatContextBuilder lineBreaksAfterPrintln(Integer value) {
        this.lineBreaksAfterPrintln = value;
        return this;
    }

    public FormatContextBuilder ifBraceSameLine(Boolean value) {
        this.ifBraceSameLine = value;
        return this;
    }

    public FormatContextBuilder ifBraceBelowLine(Boolean value) {
        this.ifBraceBelowLine = value;
        return this;
    }

    public FormatContextBuilder spaceAfterComma(Boolean value) {
        this.spaceAfterComma = value;
        return this;
    }

    public FormatContextBuilder lineBreaksBeforePrintln(Integer value) {
        this.lineBreaksBeforePrintln = value;
        return this;
    }

    public FormatContext build() {
        return new FormatContext(
                0,
                indentSpaces,
                spaceBeforeColon,
                spaceAfterColon,
                spaceAroundEquals,
                null,
                null,
                lineBreaksAfterPrintln,
                null,
                ifBraceSameLine,
                ifBraceBelowLine,
                spaceAfterComma,
                lineBreaksBeforePrintln);
    }
}
