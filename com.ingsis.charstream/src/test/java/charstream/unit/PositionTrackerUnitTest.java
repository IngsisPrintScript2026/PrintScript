/*
 * My Project
 */

package charstream.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import charstream.PositionTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PositionTrackerUnitTest {

    @Test
    @DisplayName("What: PositionTracker | When: advancing standard char | Then: column increments")
    void positionTracker_whenAdvancingStandardChar_thenIncrementsColumn() {
        // What: PositionTracker initialized at line 1, column 1
        PositionTracker tracker = new PositionTracker();
        assertEquals(1, tracker.getLine());
        assertEquals(1, tracker.getColumn());

        // When: advancing standard character 'a'
        PositionTracker next = tracker.advance('a');

        // Then: line remains 1 and column advances to 2
        assertEquals(1, next.getLine());
        assertEquals(2, next.getColumn());
    }

    @Test
    @DisplayName(
            "What: PositionTracker | When: advancing newline | Then: line increments and column"
                    + " resets")
    void positionTracker_whenAdvancingNewline_thenIncrementsLineAndResetsColumn() {
        // What: PositionTracker at line 1, column 2
        PositionTracker tracker = new PositionTracker().advance('a');

        // When: advancing newline '\n'
        PositionTracker nextLine = tracker.advance('\n');

        // Then: line becomes 2 and column resets to 1
        assertEquals(2, nextLine.getLine());
        assertEquals(1, nextLine.getColumn());
    }

    @Test
    @DisplayName(
            "What: PositionTracker | When: advancing carriage return | Then: line increments and"
                    + " column resets")
    void positionTracker_whenAdvancingCarriageReturn_thenIncrementsLineAndResetsColumn() {
        // What: PositionTracker at line 1, column 1
        PositionTracker tracker = new PositionTracker();

        // When: advancing carriage return '\r'
        PositionTracker cr = tracker.advance('\r');

        // Then: line becomes 2 and column resets to 1
        assertEquals(2, cr.getLine());
        assertEquals(1, cr.getColumn());
    }

    @Test
    @DisplayName(
            "What: PositionTracker | When: advancing CRLF sequence | Then: maintains same line"
                    + " without duplicate increment")
    void
            positionTracker_whenAdvancingCrlfSequence_thenMaintainsSameLineWithoutDuplicateIncrement() {
        // What: PositionTracker after carriage return '\r'
        PositionTracker cr = new PositionTracker().advance('\r');
        assertEquals(2, cr.getLine());

        // When: advancing newline '\n' right after '\r'
        PositionTracker crlf = cr.advance('\n');

        // Then: line remains 2 and column remains 1
        assertEquals(2, crlf.getLine());
        assertEquals(1, crlf.getColumn());
    }
}
