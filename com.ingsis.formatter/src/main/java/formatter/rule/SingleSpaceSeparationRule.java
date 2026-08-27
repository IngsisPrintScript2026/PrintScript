/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class SingleSpaceSeparationRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return Boolean.TRUE.equals(context.singleSpaceSeparation());
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        if (originalSeparator.contains("\n")) {
            return originalSeparator;
        }
        if (current != null && current.type() == TokenType.SEMICOLON) {
            return "";
        }
        return " ";
    }
}
