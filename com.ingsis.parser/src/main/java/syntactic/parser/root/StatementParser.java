/*
 * My Project
 */

package syntactic.parser.root;

import java.util.function.Predicate;
import node.Node;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import tokenstream.TokenStream;

public interface StatementParser<T extends Node> extends Parser<T> {
    boolean canStart(TokenStream stream);

    static boolean peekMatches(TokenStream stream, int offset, Predicate<Token> matcher) {
        Result<Token> peek = stream.peek(offset);
        return peek instanceof CorrectResult<Token>(Token token) && matcher.test(token);
    }
}
