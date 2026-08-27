package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class BracePositionRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        if (current == null || current.type() != TokenType.LBRACE) {
            return false;
        }
        return Boolean.TRUE.equals(context.ifBraceSameLine()) || Boolean.TRUE.equals(context.ifBraceBelowLine());
    }

    @Override
    public String formatSeparator(Token prev, Token current, String originalSeparator, FormatContext context) {
        if (Boolean.TRUE.equals(context.ifBraceSameLine())) {
            return " ";
        }
        if (Boolean.TRUE.equals(context.ifBraceBelowLine())) {
            return "\n";
        }
        return originalSeparator;
    }
}
