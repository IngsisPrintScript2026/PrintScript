/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import iterator.SafeIterator;
import metaChar.MetaCharStringBuilder;
import metaChar.MetaCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import state.State;
import version.Version;

class PositionAndMetaCharUnitTest {

    @Test
    @DisplayName(
            "What: Position | When: creating position | Then: line, column, and toString format are"
                    + " accurate")
    void position_whenCreated_thenExposesCoordinatesAndStringFormat() {
        // What: Position created at line 3, column 15
        Position pos = new Position(3, 15);

        // When: coordinates and representation are accessed
        int line = pos.line();
        int col = pos.column();
        String str = pos.toString();

        // Then: returns expected coordinates and formatted string
        assertEquals(3, line);
        assertEquals(15, col);
        assertEquals("[3:15]", str);
    }

    @Test
    @DisplayName(
            "What: MetaCharStringBuilder | When: appending characters | Then: builds correct string"
                    + " and tracks start position")
    void metaCharStringBuilder_whenAppendingCharacters_thenBuildsStringAndTracksPosition() {
        // What: empty MetaCharStringBuilder and two MetaCharacters
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        assertTrue(builder.isEmpty());
        assertEquals(-1, builder.getStartPosition().line());

        Position pos1 = new Position(1, 1);
        Position pos2 = new Position(1, 2);
        MetaCharacter mc1 = new MetaCharacter('a', pos1);
        MetaCharacter mc2 = new MetaCharacter('b', pos2);

        // When: appending mc1 and mc2
        builder.append(mc1).append(mc2);

        // Then: builder is not empty, contains "ab", and start position is [1:1]
        assertFalse(builder.isEmpty());
        assertEquals("ab", builder.buildString());
        assertEquals(1, builder.getStartPosition().line());
        assertEquals(1, builder.getStartPosition().column());
    }

    @Test
    @DisplayName(
            "What: Version | When: parsing from string | Then: parses valid versions and throws for"
                    + " invalid")
    void version_whenParsingFromString_thenResolvesValidOrThrows() {
        // What: valid version strings "1.0" and "1.1"
        // When: parsing version from string
        Version v10 = Version.fromString("1.0");
        Version v11 = Version.fromString("1.1");

        // Then: versions match enums and invalid strings throw IllegalArgumentException
        assertEquals(Version.V_1_0, v10);
        assertEquals(Version.V_1_1, v11);
        assertThrows(IllegalArgumentException.class, () -> Version.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Version.fromString("2.0"));
    }

    @Test
    @DisplayName("What: State | When: inspecting state enum | Then: contains expected enum values")
    void state_whenAccessingValues_thenContainsExpectedStates() {
        // What: State enum values
        // When: retrieving by name
        State invalid = State.valueOf("INVALID");
        State prefix = State.valueOf("PREFIX");
        State complete = State.valueOf("COMPLETE");

        // Then: values match enum constants
        assertEquals(State.INVALID, invalid);
        assertEquals(State.PREFIX, prefix);
        assertEquals(State.COMPLETE, complete);
    }

    @Test
    @DisplayName(
            "What: Result | When: creating success and failure | Then: correct type checks and"
                    + " values returned")
    void result_whenCreatingSuccessAndFailure_thenReturnsExpectedStateAndValues() {
        // What: success and failure Result instances
        Result<String> success = Result.success("ok");
        Result<String> failure = Result.failure("err");

        // When: inspecting isCorrect and casting
        boolean isSuccess = success.isCorrect();
        boolean isFailure = failure.isCorrect();

        // Then: success has "ok", failure has "err"
        assertTrue(isSuccess);
        assertEquals("ok", ((CorrectResult<String>) success).value());
        assertFalse(isFailure);
        assertEquals("err", ((IncorrectResult<String>) failure).error());
    }

    @Test
    @DisplayName(
            "What: IterationStep and SafeIterator | When: stepping through iterator | Then: yields"
                    + " values and handles unread")
    void iterationStepAndSafeIterator_whenNavigating_thenYieldsValuesAndSupportsUnread() {
        // What: dummy SafeIterator implementation
        SafeIterator<String> fakeIterator =
                new SafeIterator<String>() {
                    @Override
                    public Result<IterationStep<String>> next() {
                        return Result.failure("done");
                    }
                };

        // When: invoking unread and creating IterationStep
        fakeIterator.unread("hello");
        IterationStep<String> step = new IterationStep<>("first", fakeIterator);

        // Then: step yields value and subsequent iterator
        assertEquals("first", step.value());
        assertEquals(fakeIterator, step.next());
        assertEquals(fakeIterator, step.nextStream());
    }
}
