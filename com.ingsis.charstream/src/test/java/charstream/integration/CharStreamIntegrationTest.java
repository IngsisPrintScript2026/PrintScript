/*
 * My Project
 */

package charstream.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import charstream.CharReader;
import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import java.io.IOException;
import java.io.StringReader;
import metaChar.MetaCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;

class CharStreamIntegrationTest {

    private static class FakeFailingCharReader implements CharReader {
        private final String message;

        FakeFailingCharReader(String message) {
            this.message = message;
        }

        @Override
        public int readNextChar() throws IOException {
            throw new IOException(message);
        }

        @Override
        public void close() {}
    }

    @Test
    @DisplayName(
            "What: CharStream | When: reading multi-line stream | Then: integrates reader and"
                    + " position tracking accurately")
    void charStream_whenReadingMultiLineStream_thenIntegratesReaderAndPositionTracking() {
        // What: CharStream configured with multi-line input "hi\nworld"
        String input = "hi\nworld";
        StreamCharReader reader = new StreamCharReader(new StringReader(input));
        CharStream stream = new CharStream(reader);

        // When: reading first character 'h'
        Result<IterationStep<MetaCharacter>> step1 = stream.next();

        // Then: step1 contains 'h' at line 1, column 1
        assertTrue(step1.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c1 =
                (CorrectResult<IterationStep<MetaCharacter>>) step1;
        assertEquals('h', c1.value().value().character());
        assertEquals(1, c1.value().value().position().line());
        assertEquals(1, c1.value().value().position().column());

        // When: reading second character 'i'
        CharStream stream2 = c1.value().nextStream();
        Result<IterationStep<MetaCharacter>> step2 = stream2.next();

        // Then: step2 contains 'i' at line 1, column 2
        assertTrue(step2.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c2 =
                (CorrectResult<IterationStep<MetaCharacter>>) step2;
        assertEquals('i', c2.value().value().character());

        // When: reading third character newline '\n'
        CharStream stream3 = c2.value().nextStream();
        Result<IterationStep<MetaCharacter>> step3 = stream3.next();

        // Then: step3 contains '\n'
        assertTrue(step3.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c3 =
                (CorrectResult<IterationStep<MetaCharacter>>) step3;
        assertEquals('\n', c3.value().value().character());

        // When: reading fourth character 'w' on line 2
        CharStream stream4 = c3.value().nextStream();
        Result<IterationStep<MetaCharacter>> step4 = stream4.next();

        // Then: step4 contains 'w' at line 2, column 1
        assertTrue(step4.isCorrect());
        CorrectResult<IterationStep<MetaCharacter>> c4 =
                (CorrectResult<IterationStep<MetaCharacter>>) step4;
        assertEquals('w', c4.value().value().character());
        assertEquals(2, c4.value().value().position().line());
        assertEquals(1, c4.value().value().position().column());

        // When: unreading characters into stream4
        stream4.unread(c4.value().value());
        stream4.unread(null);
        stream4.unread(new MetaCharacter(null, new Position(1, 1)));
    }

    @Test
    @DisplayName(
            "What: CharStream | When: stream reaches EOF | Then: returns failure result with EOF"
                    + " error message")
    void charStream_whenStreamReachesEof_thenReturnsFailureWithEof() {
        // What: CharStream initialized with empty input
        StreamCharReader reader = new StreamCharReader(new StringReader(""));
        CharStream stream = new CharStream(reader);

        // When: requesting next character from empty stream
        Result<IterationStep<MetaCharacter>> step = stream.next();

        // Then: failure result returned with "EOF"
        assertFalse(step.isCorrect());
        IncorrectResult<IterationStep<MetaCharacter>> inc =
                (IncorrectResult<IterationStep<MetaCharacter>>) step;
        assertEquals("EOF", inc.error());
    }

    @Test
    @DisplayName(
            "What: CharStream | When: fake reader throws IOException | Then: returns failure result"
                    + " with IO error")
    void charStream_whenReaderThrowsIoException_thenReturnsFailureWithIoError() {
        // What: CharStream using a FakeFailingCharReader
        CharReader failingReader = new FakeFailingCharReader("Simulated disk error");
        CharStream stream = new CharStream(failingReader);

        // When: requesting next character
        Result<IterationStep<MetaCharacter>> result = stream.next();

        // Then: failure result returned with I/O error message
        assertFalse(result.isCorrect());
        IncorrectResult<IterationStep<MetaCharacter>> inc =
                (IncorrectResult<IterationStep<MetaCharacter>>) result;
        assertTrue(inc.error().contains("I/O Error: Simulated disk error"));
    }
}
