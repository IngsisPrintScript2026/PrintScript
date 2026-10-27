/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import position.SourceRange;

class SourceRangeUnitTest {

    @Test
    @DisplayName(
            "Given: a position at line 2, column 9 | When: describing it | Then: returns the"
                    + " human readable 'line L, column C' form")
    void position_whenDescribed_thenReturnsLineAndColumnText() {
        // Given
        Position position = new Position(2, 9);

        // When
        String description = position.describe();

        // Then
        assertEquals("line 2, column 9", description);
    }

    @Test
    @DisplayName(
            "Given: a range from 1:7 to 1:12 | When: converting it to text | Then: mentions both"
                    + " ends of the range")
    void sourceRange_whenBothEndsKnown_thenDescribesFromStartToEnd() {
        // Given
        SourceRange range = new SourceRange(new Position(1, 7), new Position(1, 12));

        // When
        String text = range.toString();

        // Then
        assertEquals("from line 1, column 7 to line 1, column 12", text);
    }

    @Test
    @DisplayName(
            "Given: a single position | When: building a range with at() | Then: the range starts"
                    + " and ends at that position")
    void sourceRange_whenBuiltFromSinglePosition_thenStartEqualsEnd() {
        // Given
        Position position = new Position(4, 3);

        // When
        SourceRange range = SourceRange.at(position);

        // Then
        assertEquals(position, range.start());
        assertEquals(position, range.end());
    }

    @Test
    @DisplayName(
            "Given: a range without start | When: converting it to text | Then: reports an"
                    + " unknown position")
    void sourceRange_whenStartIsNull_thenDescribesUnknownPosition() {
        // Given
        SourceRange range = new SourceRange(null, new Position(1, 1));

        // When
        String text = range.toString();

        // Then
        assertEquals("at an unknown position", text);
    }

    @Test
    @DisplayName(
            "Given: a range without end | When: converting it to text | Then: reports an unknown"
                    + " position")
    void sourceRange_whenEndIsNull_thenDescribesUnknownPosition() {
        // Given
        SourceRange range = new SourceRange(new Position(1, 1), null);

        // When
        String text = range.toString();

        // Then
        assertEquals("at an unknown position", text);
    }
}
