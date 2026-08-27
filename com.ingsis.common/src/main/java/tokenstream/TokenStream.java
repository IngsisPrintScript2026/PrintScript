/*
 * My Project
 */

package tokenstream;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.util.function.Predicate;
import result.Result;
import token.Token;
import token.TokenType;

public interface TokenStream extends SafeIterator<Token> {
    Result<IterationStep<Token>> consume();

    Result<IterationStep<Token>> consume(TokenType expectedType);

    Result<IterationStep<Token>> consume(Predicate<Token> matcher);

    Result<Token> peek(int offset);

    boolean isEmpty();

    int pointer();
}
