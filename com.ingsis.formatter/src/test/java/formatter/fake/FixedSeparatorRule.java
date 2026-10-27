/*
 * My Project
 */

package formatter.fake;

import formatter.FormatContext;
import formatter.rule.FormattingRule;
import token.Token;

/** Rule stub that always applies and replaces every separator with a fixed text. */
public record FixedSeparatorRule(String separator) implements FormattingRule {
    @Override
    public boolean applies(Token prev, Token current, FormatContext context) {
        return true;
    }

    @Override
    public String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context) {
        return separator;
    }
}
