/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

/**
 * Mandatory: every statement ends its line after the ';', and block contents start and end on their
 * own lines (so they can be indented).
 */
public class LineBreakAfterStatementRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        if (prev == null || current == null) {
            return false;
        }
        return isType(prev, TokenType.SEMICOLON)
                || (isType(prev, TokenType.LBRACE) && !isType(current, TokenType.RBRACE))
                || (isType(current, TokenType.RBRACE) && !isType(prev, TokenType.LBRACE))
                || (isType(prev, TokenType.RBRACE) && !isType(current, TokenType.ELSE));
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        return originalSeparator.contains("\n") ? originalSeparator : "\n";
    }

    private static boolean isType(Token token, TokenType type) {
        return token.type() == type;
    }
}
