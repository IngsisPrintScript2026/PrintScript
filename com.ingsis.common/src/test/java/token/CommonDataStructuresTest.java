/*
 * My Project
 */

package token;

import static org.junit.jupiter.api.Assertions.*;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.util.Optional;
import metaChar.MetaCharStringBuilder;
import metaChar.MetaCharacter;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import state.State;
import token.tokenize.TokenizeResult;
import version.Version;

class CommonDataStructuresTest {

    @Test
    void testPosition() {
        Position pos = new Position(3, 15);
        assertEquals(3, pos.line());
        assertEquals(15, pos.column());
        assertEquals("[3:15]", pos.toString());
    }

    @Test
    void testMetaCharacterAndBuilder() {
        Position pos = new Position(1, 1);
        MetaCharacter mc1 = new MetaCharacter('a', pos);
        MetaCharacter mc2 = new MetaCharacter('b', new Position(1, 2));

        assertEquals('a', mc1.character());
        assertEquals(pos, mc1.position());

        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        assertTrue(builder.isEmpty());
        assertEquals(-1, builder.getStartPosition().line());

        builder.append(mc1).append(mc2);
        assertFalse(builder.isEmpty());
        assertEquals("ab", builder.buildString());
        assertEquals(1, builder.getStartPosition().line());
        assertEquals(1, builder.getStartPosition().column());
    }

    @Test
    void testVersion() {
        assertEquals(Version.V_1_0, Version.fromString("1.0"));
        assertEquals(Version.V_1_1, Version.fromString("1.1"));
        assertThrows(IllegalArgumentException.class, () -> Version.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Version.fromString("2.0"));
    }

    @Test
    void testStateEnum() {
        assertEquals(State.INVALID, State.valueOf("INVALID"));
        assertEquals(State.PREFIX, State.valueOf("PREFIX"));
        assertEquals(State.COMPLETE, State.valueOf("COMPLETE"));
    }

    @Test
    void testResultClasses() {
        Result<String> success = Result.success("ok");
        assertTrue(success.isCorrect());
        CorrectResult<String> correct = (CorrectResult<String>) success;
        assertEquals("ok", correct.value());

        Result<String> failure = Result.failure("err");
        assertFalse(failure.isCorrect());
        IncorrectResult<String> incorrect = (IncorrectResult<String>) failure;
        assertEquals("err", incorrect.error());
    }

    @Test
    void testTokenAndTokenInterface() {
        Position p1 = new Position(1, 1);
        Position p2 = new Position(1, 4);
        Token tok = new Token(TokenType.LET, "let", p1, p2);

        assertEquals(TokenType.LET, tok.type());
        assertEquals("let", tok.value());
        assertEquals(p1, tok.startPosition());
        assertEquals(p2, tok.endPosition());
        assertEquals(1, tok.line());
        assertEquals(1, tok.column());
        assertFalse(tok.isNull());

        Token tok2 = new Token(TokenType.IDENTIFIER, "foo", null);
        assertEquals(-1, tok2.line());
        assertEquals(-1, tok2.column());
    }

    @Test
    void testSymbolType() {
        Token eqTok = new Token(TokenType.EQUAL, "=", new Position(1, 1));
        Token colonTok = new Token(TokenType.COLON, ":", new Position(1, 2));

        assertTrue(SymbolType.isSymbol(eqTok, SymbolType.EQUAL));
        assertFalse(SymbolType.isSymbol(eqTok, SymbolType.COLON));
        assertFalse(SymbolType.isSymbol(null, SymbolType.EQUAL));
        assertFalse(SymbolType.isSymbol(eqTok, null));

        assertEquals(Optional.of(SymbolType.EQUAL), SymbolType.fromToken(eqTok));
        assertEquals(Optional.empty(), SymbolType.fromToken(null));
        assertEquals(
                Optional.empty(),
                SymbolType.fromToken(new Token(TokenType.IDENTIFIER, "abc", new Position(1, 1))));

        assertEquals(Optional.of(SymbolType.COLON), SymbolType.fromTokenType(TokenType.COLON));
        assertEquals(Optional.empty(), SymbolType.fromTokenType(null));
        assertEquals(Optional.empty(), SymbolType.fromTokenType(TokenType.LET));

        assertEquals(TokenType.EQUAL, SymbolType.EQUAL.tokenType());
        assertEquals("=", SymbolType.EQUAL.symbol());
    }

    @Test
    void testTokenizeResult() {
        Token tok = new Token(TokenType.LET, "let", new Position(1, 1));
        TokenizeResult complete = new TokenizeResult.Complete(tok);
        TokenizeResult prefix = new TokenizeResult.Prefix();
        TokenizeResult invalid = new TokenizeResult.Invalid("bad");

        assertInstanceOf(TokenizeResult.Complete.class, complete);
        assertInstanceOf(TokenizeResult.Prefix.class, prefix);
        assertInstanceOf(TokenizeResult.Invalid.class, invalid);
        assertEquals(tok, ((TokenizeResult.Complete) complete).token());
        assertEquals("bad", ((TokenizeResult.Invalid) invalid).reason());
    }

    @Test
    void testIterationStepAndSafeIterator() {
        SafeIterator<String> dummyIterator =
                new SafeIterator<String>() {
                    @Override
                    public Result<IterationStep<String>> next() {
                        return Result.failure("done");
                    }
                };
        dummyIterator.unread("hello"); // test default unread

        IterationStep<String> step = new IterationStep<>("first", dummyIterator);
        assertEquals("first", step.value());
        assertEquals(dummyIterator, step.next());
        assertEquals(dummyIterator, step.nextStream());
    }
}
