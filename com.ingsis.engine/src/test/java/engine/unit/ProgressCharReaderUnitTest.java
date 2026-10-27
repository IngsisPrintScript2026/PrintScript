/*
 * My Project
 */

package engine.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.fake.FakeCharReader;
import engine.fake.SpyProgressListener;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import progress.ProgressCharReader;

class ProgressCharReaderUnitTest {

    @Test
    @DisplayName(
            "Given: a reader over 'abc' | When: reading every character | Then: the listener is"
                    + " told the running count 1, 2, 3")
    void progressReader_whenReadingChars_thenReportsRunningCount() throws Exception {
        // Given
        SpyProgressListener listener = new SpyProgressListener();
        ProgressCharReader reader = new ProgressCharReader(new FakeCharReader("abc"), listener);

        // When
        String read =
                ""
                        + (char) reader.readNextChar()
                        + (char) reader.readNextChar()
                        + (char) reader.readNextChar();

        // Then
        assertEquals("abc", read);
        assertEquals(List.of(1L, 2L, 3L), listener.reported());
    }

    @Test
    @DisplayName(
            "Given: an exhausted reader | When: reading past the end | Then: returns -1 and"
                    + " does not report progress")
    void progressReader_whenReadingAtEnd_thenDoesNotReport() throws Exception {
        // Given
        SpyProgressListener listener = new SpyProgressListener();
        ProgressCharReader reader = new ProgressCharReader(new FakeCharReader(""), listener);

        // When
        int read = reader.readNextChar();

        // Then
        assertEquals(-1, read);
        assertTrue(listener.reported().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a character that the lexer read as lookahead | When: it is unread and read"
                    + " again | Then: it is counted only once")
    void progressReader_whenUnreadingLookahead_thenCountsCharOnce() throws Exception {
        // Given
        SpyProgressListener listener = new SpyProgressListener();
        ProgressCharReader reader = new ProgressCharReader(new FakeCharReader("ab"), listener);
        reader.readNextChar();
        int lookahead = reader.readNextChar();

        // When
        reader.unread(lookahead);
        reader.readNextChar();

        // Then
        assertEquals(2L, listener.lastReported());
    }

    @Test
    @DisplayName("Given: a progress reader | When: closing it | Then: the wrapped reader is closed")
    void progressReader_whenClosed_thenClosesDelegate() throws Exception {
        // Given
        FakeCharReader delegate = new FakeCharReader("x");
        ProgressCharReader reader = new ProgressCharReader(delegate, new SpyProgressListener());

        // When
        reader.close();

        // Then
        assertTrue(delegate.isClosed());
    }
}
