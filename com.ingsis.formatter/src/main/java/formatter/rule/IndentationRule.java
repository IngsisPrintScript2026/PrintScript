package formatter.rule;

import formatter.FormatContext;
import token.Token;
import token.TokenType;

public class IndentationRule implements FormattingRule {
    private int currentDepth = 0;

    public void setDepth(int depth) {
        this.currentDepth = depth;
    }

    public int getDepth() {
        return currentDepth;
    }

    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return context.indentSpaces() != null;
    }

    @Override
    public String formatSeparator(Token prev, Token current, String originalSeparator, FormatContext context) {
        if (!originalSeparator.contains("\n")) {
            return originalSeparator;
        }

        int targetDepth = currentDepth;
        if (current != null && current.type() == TokenType.RBRACE) {
            targetDepth = Math.max(0, currentDepth - 1);
        }

        int indentSize = context.indentSpaces();
        String indent = " ".repeat(Math.max(0, targetDepth * indentSize));

        // Preserve all newlines in originalSeparator, replace the final trailing spaces with indent
        int lastNewline = originalSeparator.lastIndexOf('\n');
        String newlines = originalSeparator.substring(0, lastNewline + 1);
        return newlines + indent;
    }
}
