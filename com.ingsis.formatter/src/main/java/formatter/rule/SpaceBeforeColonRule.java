package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class SpaceBeforeColonRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return current != null && current.type() == TokenType.COLON && context.spaceBeforeColon() != null;
    }

    @Override
    public String formatSeparator(Token prev, Token current, String originalSeparator, FormatContext context) {
        return context.spaceBeforeColon() ? " " : "";
    }
}
