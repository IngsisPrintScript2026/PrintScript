/*
 * My Project
 */

package parser.support;

import charstream.CharStream;
import charstream.StreamCharReader;
import java.io.StringReader;
import lexer.Lexer;
import position.Position;
import token.Token;
import token.TokenType;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;

public final class Sources {
    private Sources() {}

    public static TokenStream tokens(String source) {
        return new LazyTokenStream(
                new Lexer(new CharStream(new StreamCharReader(new StringReader(source)))));
    }

    public static Token token(TokenType type, String value, int column) {
        return new Token(
                type, value, new Position(1, column), new Position(1, column + value.length() - 1));
    }
}
