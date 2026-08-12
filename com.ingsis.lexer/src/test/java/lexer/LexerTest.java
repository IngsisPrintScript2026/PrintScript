package lexer;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import iterator.SafeIterator;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {

    @Test
    void testLexerTokenizesPrintScriptExampleFromPDF() {
        String code = """
                let a: number = 12;
                let b: number = 4;
                let c: number = a / b;
                println("Result: " + c);
                """;

        StreamCharReader reader = new StreamCharReader(new StringReader(code));
        CharStream charStream = new CharStream(reader);

        SafeIterator<Token> lexer = new Lexer(charStream);

        List<Token> tokens = new ArrayList<>();
        SafeIterator<Token> currLexer = lexer;

        boolean keepGoing = true;
        while (keepGoing) {
            Result<IterationStep<Token>> res = currLexer.next();
            switch (res) {
                case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                    tokens.add(step.value());
                    currLexer = step.nextStream();
                }
                case IncorrectResult<IterationStep<Token>> failure -> keepGoing = false;
            }
        }

        assertEquals(30, tokens.size());

        // Line 1: let a: number = 12;
        assertEquals(TokenType.LET, tokens.get(0).type());
        assertEquals("let", tokens.get(0).value());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("a", tokens.get(1).value());
        assertEquals(TokenType.COLON, tokens.get(2).type());
        assertEquals(":", tokens.get(2).value());
        assertEquals(TokenType.NUMBER, tokens.get(3).type());
        assertEquals("number", tokens.get(3).value());
        assertEquals(TokenType.EQUAL, tokens.get(4).type());
        assertEquals("=", tokens.get(4).value());
        assertEquals(TokenType.NUMBER_LITERAL, tokens.get(5).type());
        assertEquals("12", tokens.get(5).value());
        assertEquals(TokenType.SEMICOLON, tokens.get(6).type());

        // Line 3: let c: number = a / b;
        assertEquals(TokenType.SLASH, tokens.get(20).type());
        assertEquals("/", tokens.get(20).value());

        // Line 4: println("Result: " + c);
        assertEquals(TokenType.PRINTLN, tokens.get(23).type());
        assertEquals("println", tokens.get(23).value());
        assertEquals(TokenType.LPAREN, tokens.get(24).type());
        assertEquals("(", tokens.get(24).value());
        assertEquals(TokenType.STRING_LITERAL, tokens.get(25).type());
        assertEquals("\"Result: \"", tokens.get(25).value());
        assertEquals(TokenType.PLUS, tokens.get(26).type());
        assertEquals("+", tokens.get(26).value());
        assertEquals(TokenType.IDENTIFIER, tokens.get(27).type());
        assertEquals("c", tokens.get(27).value());
        assertEquals(TokenType.RPAREN, tokens.get(28).type());
        assertEquals(")", tokens.get(28).value());
        assertEquals(TokenType.SEMICOLON, tokens.get(29).type());
        assertEquals(";", tokens.get(29).value());
    }
}
