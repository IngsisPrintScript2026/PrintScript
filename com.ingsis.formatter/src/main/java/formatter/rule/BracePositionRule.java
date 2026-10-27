/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

/**
 * The '{' of an if/else block goes on the same line as its header (default), unless
 * if-brace-below-line is explicitly enabled. "} else" follows the same placement.
 */
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
