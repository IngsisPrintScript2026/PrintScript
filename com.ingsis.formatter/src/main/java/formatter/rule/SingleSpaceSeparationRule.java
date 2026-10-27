/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

/**
 * Mandatory: at most one space between tokens on the same line, none before a ';'. With
 * mandatory-single-space-separation enabled, exactly one space between every pair of tokens.
 */
public class SingleSpaceSeparationRule implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return true;
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        if (originalSeparator.contains("\n")) {
            return originalSeparator;
        }
        if (current != null && current.type() == TokenType.SEMICOLON) {
            return "";
        }
        boolean exactlyOne = Boolean.TRUE.equals(context.singleSpaceSeparation());
        return exactlyOne || !originalSeparator.isEmpty() ? " " : "";
    }
}
