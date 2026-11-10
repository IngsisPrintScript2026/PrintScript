/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class LinesBeforePrintlnRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return context.lineBreaksBeforePrintln() != null
                && current != null
                && current.type() == TokenType.PRINTLN
                && prev != null
                && (prev.type() == TokenType.SEMICOLON
                        || prev.type() == TokenType.LBRACE
                        || prev.type() == TokenType.RBRACE);
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        return "\n".repeat(context.lineBreaksBeforePrintln() + 1);
    }
}
