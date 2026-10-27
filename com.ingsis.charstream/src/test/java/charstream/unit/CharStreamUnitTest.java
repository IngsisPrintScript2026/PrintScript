/*
 * My Project
 */

package charstream.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import charstream.CharReader;
import charstream.CharStream;
import iterator.IterationStep;
import iterator.SafeIterator;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import metaChar.MetaCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;

class CharStreamUnitTest {

    /** In-memory reader that supports pushing chars back, like a PushbackReader. */
    private static final class FakeCharReader implements CharReader {
        private final Deque<Integer> pending = new ArrayDeque<>();

        FakeCharReader(String text) {
            text.chars().forEach(pending::addLast);
        }

        @Override
        public int readNextChar() {
            return pending.isEmpty() ? -1 : pending.removeFirst();
        }

        @Override
        public void unread(int c) {
            pending.addFirst(c);
        }

        @Override
        public void close() {}
    }

    /** Reader whose unread always fails, to check that the stream swallows the failure. */
    private static final class StubFailingUnreadReader implements CharReader {
        @Override
        public int readNextChar() {
            return 'a';
        }

        @Override
        public void unread(int c) throws IOException {
            throw new IOException("pushback buffer full");
        }

        @Override
        public void close() {}
    }

    private static IterationStep<MetaCharacter> read(SafeIterator<MetaCharacter> stream) {
        Result<IterationStep<MetaCharacter>> result = stream.next();
        assertTrue(result.isCorrect());
        return ((CorrectResult<IterationStep<MetaCharacter>>) result).value();
    }

    @Test
    @DisplayName(
            "Given: a stream that already read 'a' | When: unreading 'a' and reading again | Then:"
                    + " 'a' is returned again")
    void charStream_whenCharIsUnread_thenNextReadReturnsItAgain() {
        // Given
        CharStream stream = new CharStream(new FakeCharReader("ab"));
        IterationStep<MetaCharacter> first = read(stream);
        SafeIterator<MetaCharacter> afterA = first.nextStream();

        // When
        afterA.unread(first.value());
        IterationStep<MetaCharacter> again = read(afterA);

        // Then
        assertEquals('a', again.value().character());
    }

    @Test
    @DisplayName(
            "Given: a stream | When: unreading null | Then: nothing is pushed back and the next"
                    + " char is unchanged")
    void charStream_whenUnreadingNull_thenStreamIsUnchanged() {
        // Given
        CharStream stream = new CharStream(new FakeCharReader("z"));

        // When
        stream.unread(null);
        IterationStep<MetaCharacter> step = read(stream);

        // Then
        assertEquals('z', step.value().character());
    }

    @Test
    @DisplayName(
            "Given: a reader whose unread throws IOException | When: unreading a char | Then: the"
                    + " error is swallowed and the stream keeps working")
    void charStream_whenReaderUnreadFails_thenErrorIsSwallowed() {
        // Given
        CharStream stream = new CharStream(new StubFailingUnreadReader());

        // When
        stream.unread(new MetaCharacter('x', new Position(1, 1)));
        IterationStep<MetaCharacter> step = read(stream);

        // Then
        assertEquals('a', step.value().character());
    }

    @Test
    @DisplayName(
            "Given: source 'a\\r"
                    + "\\n"
                    + "b' (Windows line ending) | When: reading up to 'b' | Then: 'b' is at line 2,"
                    + " column 1")
    void charStream_whenSourceUsesCrLf_thenNextLineStartsAtColumnOne() {
        // Given
        SafeIterator<MetaCharacter> stream = new CharStream(new FakeCharReader("a\r\nb"));

        // When
        for (int i = 0; i < 3; i++) {
            stream = read(stream).nextStream();
        }
        MetaCharacter b = read(stream).value();

        // Then
        assertEquals(new Position(2, 1), b.position());
    }

    @Test
    @DisplayName(
            "Given: source 'ab' | When: reading the second char | Then: it is at line 1, column"
                    + " 2")
    void charStream_whenReadingSecondChar_thenColumnAdvances() {
        // Given
        CharStream stream = new CharStream(new FakeCharReader("ab"));

        // When
        MetaCharacter second = read(read(stream).nextStream()).value();

        // Then
        assertEquals(new Position(1, 2), second.position());
    }
}
