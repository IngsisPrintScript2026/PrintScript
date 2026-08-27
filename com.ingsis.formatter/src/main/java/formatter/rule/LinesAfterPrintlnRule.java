/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class LinesAfterPrintlnRule implements FormattingRule {
    private boolean isAfterPrintln = false;

    public void setAfterPrintln(boolean afterPrintln) {
        this.isAfterPrintln = afterPrintln;
    }

    public boolean isAfterPrintln() {
        return isAfterPrintln;
    }

    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return prev != null
                && prev.type() == TokenType.SEMICOLON
                && isAfterPrintln
                && context.lineBreaksAfterPrintln() != null;
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        int breaks = context.lineBreaksAfterPrintln();
        isAfterPrintln = false;
        return "\n".repeat(breaks + 1);
    }
}
