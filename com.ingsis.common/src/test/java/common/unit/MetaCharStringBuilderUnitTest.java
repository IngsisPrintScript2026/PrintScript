/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import metaChar.MetaCharStringBuilder;
import metaChar.MetaCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;

class MetaCharStringBuilderUnitTest {

    @Test
    @DisplayName(
            "Given: an empty builder | When: asking its end position | Then: returns the -1,-1"
                    + " sentinel")
    void metaCharStringBuilder_whenEmpty_thenEndPositionIsSentinel() {
        // Given
        MetaCharStringBuilder builder = new MetaCharStringBuilder();

        // When
        Position end = builder.getEndPosition();

        // Then
        assertEquals(new Position(-1, -1), end);
    }

    @Test
    @DisplayName(
            "Given: a builder with three chars appended | When: asking its end position | Then:"
                    + " returns the position of the last char")
    void metaCharStringBuilder_whenSeveralCharsAppended_thenEndIsLastCharPosition() {
        // Given
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        builder.append(new MetaCharacter('l', new Position(1, 1)))
                .append(new MetaCharacter('e', new Position(1, 2)))
                .append(new MetaCharacter('t', new Position(1, 3)));

        // When
        Position end = builder.getEndPosition();

        // Then
        assertEquals(new Position(1, 3), end);
        assertEquals(new Position(1, 1), builder.getStartPosition());
    }

    @Test
    @DisplayName(
            "Given: a builder with a single char | When: asking start and end | Then: both are the"
                    + " position of that char")
    void metaCharStringBuilder_whenSingleCharAppended_thenStartEqualsEnd() {
        // Given
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        builder.append(new MetaCharacter(';', new Position(2, 4)));

        // When
        Position end = builder.getEndPosition();

        // Then
        assertEquals(builder.getStartPosition(), end);
    }

    @Test
    @DisplayName(
            "Given: a lexeme spanning a line break | When: asking its end position | Then: points"
                    + " to the line of the last char")
    void metaCharStringBuilder_whenLexemeSpansLines_thenEndIsOnLastLine() {
        // Given
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        builder.append(new MetaCharacter('"', new Position(1, 10)))
                .append(new MetaCharacter('\n', new Position(1, 11)))
                .append(new MetaCharacter('"', new Position(2, 1)));

        // When
        Position end = builder.getEndPosition();

        // Then
        assertEquals(new Position(2, 1), end);
    }
}
