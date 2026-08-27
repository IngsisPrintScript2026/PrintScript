/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class LineBreakAfterStatementRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return prev != null
                && prev.type() == TokenType.SEMICOLON
                && Boolean.TRUE.equals(context.lineBreakAfterStatement());
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        if (!originalSeparator.contains("\n")) {
            return "\n";
        }
        return originalSeparator;
    }
}
