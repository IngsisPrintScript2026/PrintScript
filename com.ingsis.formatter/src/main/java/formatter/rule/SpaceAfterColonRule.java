package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class SpaceAfterColonRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return prev != null && prev.type() == TokenType.COLON && context.spaceAfterColon() != null;
    }

    @Override
    public String formatSeparator(Token prev, Token current, String originalSeparator, FormatContext context) {
        return context.spaceAfterColon() ? " " : "";
    }
}
