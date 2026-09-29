/*
 * My Project
 */

package charstream.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import charstream.CharReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FakeCharReaderUnitTest {

    private static class FakeCharReader implements CharReader {
        private final int fixedChar;

        public FakeCharReader(int fixedChar) {
            this.fixedChar = fixedChar;
        }

        @Override
        public int readNextChar() {
            return fixedChar;
        }

        @Override
        public void close() {}
    }

    @Test
    @DisplayName(
            "What: FakeCharReader | When: invoking default unread method | Then: subsequent read"
                    + " returns expected char")
    void fakeCharReader_whenInvokingDefaultUnread_thenSubsequentReadReturnsExpectedChar()
            throws Exception {
        // What: FakeCharReader returning 42
        FakeCharReader fakeReader = new FakeCharReader(42);

        // When: invoking unread default implementation
        fakeReader.unread('a');

        // Then: readNextChar returns the constant value
        assertEquals(42, fakeReader.readNextChar());
    }
}
