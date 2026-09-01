/*
 * My Project
 */

package charstream;

import static org.junit.jupiter.api.Assertions.*;

import iterator.IterationStep;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import metaChar.MetaCharacter;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;

class CharStreamTest {

    @Test
    void testPositionTrackerAdvancement() {
        PositionTracker tracker = new PositionTracker();
        assertEquals(1, tracker.getLine());
        assertEquals(1, tracker.getColumn());

        // Advance standard character
        PositionTracker next = tracker.advance('a');
        assertEquals(1, next.getLine());
        assertEquals(2, next.getColumn());

        // Advance newline \n
        PositionTracker nextLine = next.advance('\n');
        assertEquals(2, nextLine.getLine());
        assertEquals(1, nextLine.getColumn());

        // Advance carriage return \r
        PositionTracker cr = nextLine.advance('\r');
        assertEquals(3, cr.getLine());
        assertEquals(1, cr.getColumn());

        // Advance \n following \r (CRLF sequence)
        PositionTracker crlf = cr.advance('\n');
        assertEquals(3, crlf.getLine());
        assertEquals(1, crlf.getColumn());
    }

    @Test
    void testStreamCharReader() throws Exception {
        StringReader stringReader = new StringReader("ab");
        StreamCharReader charReader = new StreamCharReader(stringReader);

        assertEquals('a', charReader.readNextChar());
        charReader.unread('a');
        assertEquals('a', charReader.readNextChar());
        assertEquals('b', charReader.readNextChar());
        assertEquals(-1, charReader.readNextChar());
        charReader.unread(-1);

        charReader.close();

        // Test with BufferedReader and PushbackReader
        BufferedReader br = new BufferedReader(new StringReader("x"));
        try (StreamCharReader r2 = new StreamCharReader(br)) {
            assertEquals('x', r2.readNextChar());
        }

        PushbackReader pr = new PushbackReader(new StringReader("y"));
        try (StreamCharReader r3 = new StreamCharReader(pr)) {
            assertEquals('y', r3.readNextChar());
        }
    }

    @Test
    void testCharStreamReadsCharacters() {
        String input = "hi\nworld";
        StreamCharReader reader = new StreamCharReader(new StringReader(input));
        CharStream stream = new CharStream(reader);

        Result<IterationStep<MetaCharacter>> step1 = stream.next();
        assertTrue(step1.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c1 =
                (CorrectResult<IterationStep<MetaCharacter>>) step1;
        assertEquals('h', c1.value().value().character());
        assertEquals(1, c1.value().value().position().line());
        assertEquals(1, c1.value().value().position().column());

        CharStream stream2 = c1.value().nextStream();
        Result<IterationStep<MetaCharacter>> step2 = stream2.next();
        assertTrue(step2.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c2 =
                (CorrectResult<IterationStep<MetaCharacter>>) step2;
        assertEquals('i', c2.value().value().character());

        CharStream stream3 = c2.value().nextStream();
        Result<IterationStep<MetaCharacter>> step3 = stream3.next();
        assertTrue(step3.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c3 =
                (CorrectResult<IterationStep<MetaCharacter>>) step3;
        assertEquals('\n', c3.value().value().character());

        CharStream stream4 = c3.value().nextStream();
        Result<IterationStep<MetaCharacter>> step4 = stream4.next();
        assertTrue(step4.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c4 =
                (CorrectResult<IterationStep<MetaCharacter>>) step4;
        assertEquals('w', c4.value().value().character());
        assertEquals(2, c4.value().value().position().line());
        assertEquals(1, c4.value().value().position().column());

        // unread
        stream4.unread(c4.value().value());
        stream4.unread(null);
        stream4.unread(new MetaCharacter(null, new Position(1, 1)));
    }

    @Test
    void testCharStreamEOF() {
        StreamCharReader reader = new StreamCharReader(new StringReader(""));
        CharStream stream = new CharStream(reader);
        Result<IterationStep<MetaCharacter>> step = stream.next();
        assertFalse(step.isCorrect());
        IncorrectResult<IterationStep<MetaCharacter>> inc =
                (IncorrectResult<IterationStep<MetaCharacter>>) step;
        assertEquals("EOF", inc.error());
    }

    @Test
    void testCharStreamIOException() throws Exception {
        CharReader failingReader =
                new CharReader() {
                    @Override
                    public int readNextChar() throws IOException {
                        throw new IOException("Simulated disk error");
                    }

                    @Override
                    public void close() {}
                };
        CharStream stream = new CharStream(failingReader);
        Result<IterationStep<MetaCharacter>> result = stream.next();
        assertFalse(result.isCorrect());
        IncorrectResult<IterationStep<MetaCharacter>> inc =
                (IncorrectResult<IterationStep<MetaCharacter>>) result;
        assertTrue(inc.error().contains("I/O Error: Simulated disk error"));
    }

    @Test
    void testCharReaderDefaultUnread() throws Exception {
        CharReader simpleReader =
                new CharReader() {
                    @Override
                    public int readNextChar() {
                        return 42;
                    }

                    @Override
                    public void close() {}
                };
        simpleReader.unread('a');
        assertEquals(42, simpleReader.readNextChar());
    }
}
