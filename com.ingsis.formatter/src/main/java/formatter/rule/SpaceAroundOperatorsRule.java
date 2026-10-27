/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

/** Mandatory: exactly one space before and after every arithmetic operator. */
public class SpaceAroundOperatorsRule implements FormattingRule {
    private boolean isOperator(Token token) {
        if (token == null) {
            return false;
        }
        TokenType type = token.type();
        return type == TokenType.PLUS
                || type == TokenType.MINUS
                || type == TokenType.STAR
                || type == TokenType.SLASH;
    }

    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return isOperator(prev) || isOperator(current);
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        return " ";
    }
}
