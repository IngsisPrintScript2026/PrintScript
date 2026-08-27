/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class SpaceAroundOperatorsRule implements FormattingRule {
    private boolean isOperator(TokenType type) {
        return type == TokenType.PLUS
                || type == TokenType.MINUS
                || type == TokenType.STAR
                || type == TokenType.SLASH;
    }

    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        if (context.spaceAroundOperators() == null) {
            return false;
        }
        return (prev != null && isOperator(prev.type()))
                || (current != null && isOperator(current.type()));
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        return Boolean.TRUE.equals(context.spaceAroundOperators()) ? " " : "";
    }
}
