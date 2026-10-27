/*
 * My Project
 */

package engine.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import progress.ConsoleProgressReporter;

class ConsoleProgressReporterUnitTest {
    private ByteArrayOutputStream console;
    private PrintStream out;

    @BeforeEach
    void setUp() {
        console = new ByteArrayOutputStream();
        out = new PrintStream(console, true, StandardCharsets.UTF_8);
    }

    private String printed() {
        return console.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName(
            "Given: a source of 200 characters | When: 50 were read | Then: shows 25% on the"
                    + " progress line")
    void reporter_whenQuarterRead_thenShowsTwentyFivePercent() {
        // Given
        ConsoleProgressReporter reporter = new ConsoleProgressReporter(out, 200);

        // When
        reporter.onCharactersRead(50);

        // Then
        assertEquals("\rParsing: 25%", printed());
    }

    @Test
    @DisplayName(
            "Given: a percentage already shown | When: more characters arrive within the same"
                    + " percentage | Then: the line is not redrawn")
    void reporter_whenPercentageDoesNotChange_thenDoesNotRedraw() {
        // Given
        ConsoleProgressReporter reporter = new ConsoleProgressReporter(out, 1000);
        reporter.onCharactersRead(10);

        // When
        reporter.onCharactersRead(11);

        // Then
        assertEquals("\rParsing: 1%", printed());
    }

    @Test
    @DisplayName(
            "Given: a progress line on screen | When: clearing it | Then: the line is erased so"
                    + " program output does not mix with it")
    void reporter_whenCleared_thenErasesLine() {
        // Given
        ConsoleProgressReporter reporter = new ConsoleProgressReporter(out, 10);
        reporter.onCharactersRead(5);

        // When
        reporter.clear();

        // Then
        assertTrue(printed().endsWith("\r"));
        assertTrue(printed().contains("\r" + " ".repeat(13) + "\r"));
    }

    @Test
    @DisplayName(
            "Given: a cleared progress line | When: more characters are read | Then: the line is"
                    + " drawn again")
    void reporter_whenReadAfterClear_thenRedraws() {
        // Given
        ConsoleProgressReporter reporter = new ConsoleProgressReporter(out, 10);
        reporter.onCharactersRead(5);
        reporter.clear();

        // When
        reporter.onCharactersRead(5);

        // Then
        assertTrue(printed().endsWith("\rParsing: 50%"));
    }

    @Test
    @DisplayName(
            "Given: parsing that started | When: finishing | Then: shows 100% and ends the line")
    void reporter_whenFinished_thenShowsHundredPercentAndNewLine() {
        // Given
        ConsoleProgressReporter reporter = new ConsoleProgressReporter(out, 10);
        reporter.onCharactersRead(3);

        // When
        reporter.finish();

        // Then
        assertTrue(printed().endsWith("\rParsing: 100%" + System.lineSeparator()));
    }

    @Test
    @DisplayName(
            "Given: nothing was parsed (empty source) | When: finishing or clearing | Then:"
                    + " nothing is printed")
    void reporter_whenNothingWasRead_thenPrintsNothing() {
        // Given
        ConsoleProgressReporter reporter = new ConsoleProgressReporter(out, 0);

        // When
        reporter.clear();
        reporter.finish();

        // Then
        assertEquals("", printed());
    }
}
