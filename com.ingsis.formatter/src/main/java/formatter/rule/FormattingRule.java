/*
 * My Project
 */

package formatter.rule;

import formatter.FormatContext;
import token.Token;

public interface FormattingRule {
    boolean applies(Token prev, Token current, FormatContext context);

    String formatSeparator(
            Token prev, Token current, String originalSeparator, FormatContext context);
}
