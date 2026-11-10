/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class BracePositionRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        if (current == null) {
            return false;
        }
        return current.type() == TokenType.LBRACE
                || (current.type() == TokenType.ELSE
                        && prev != null
                        && prev.type() == TokenType.RBRACE);
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        boolean belowLine =
                Boolean.TRUE.equals(context.ifBraceBelowLine())
                        && !Boolean.TRUE.equals(context.ifBraceSameLine());
        return belowLine ? "\n" : " ";
    }
}
