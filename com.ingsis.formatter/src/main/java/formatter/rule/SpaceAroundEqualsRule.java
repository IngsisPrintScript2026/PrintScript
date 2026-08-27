/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class SpaceAroundEqualsRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        if (context.spaceAroundEquals() == null) {
            return false;
        }
        return (prev != null && prev.type() == TokenType.EQUAL)
                || (current != null && current.type() == TokenType.EQUAL);
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        return Boolean.TRUE.equals(context.spaceAroundEquals()) ? " " : "";
    }
}
