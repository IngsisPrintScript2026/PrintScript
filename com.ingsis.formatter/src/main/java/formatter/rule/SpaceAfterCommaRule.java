/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class SpaceAfterCommaRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return prev != null && prev.type() == TokenType.COMMA && context.spaceAfterComma() != null;
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        return context.spaceAfterComma() ? " " : "";
    }
}
