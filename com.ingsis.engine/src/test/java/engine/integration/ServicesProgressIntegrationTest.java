/*
 * My Project
 */

package engine.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.fake.SpyProgressListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.Result;
import service.ExecuteService;
import service.FormatService;
import service.LintService;
import service.ValidationService;
import version.Version;

class ServicesProgressIntegrationTest {
    private static final String SOURCE = "let a: number = 1;\nprintln(a);\n";

    private static InputStream source(String code) {
        return new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isIncreasing(List<Long> values) {
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) {
                return false;
            }
        }
        return true;
    }

    @Test
    @DisplayName(
            "Given: a valid program and a progress listener | When: validating | Then: progress"
                    + " grows character by character until the whole source was consumed")
    void validation_whenRunning_thenReportsProgressUpToSourceLength() {
        // Given
        SpyProgressListener progress = new SpyProgressListener();

        // When
        Result<String> result =
                new ValidationService(progress).validate(Version.V_1_0, source(SOURCE));

        // Then
        assertTrue(result.isCorrect());
        assertEquals(SOURCE.length(), progress.lastReported());
        assertTrue(isIncreasing(progress.reported()));
    }

    @Test
    @DisplayName(
            "Given: a program whose second statement is invalid | When: validating | Then:"
                    + " parsing stops there, before consuming the rest of the source")
    void validation_whenErrorInTheMiddle_thenStopsReadingTheSource() {
        // Given
        String code = "let a: number = 1;\nlet b number = 2;\n" + "println(a);\n".repeat(50);
        SpyProgressListener progress = new SpyProgressListener();

        // When
        Result<String> result =
                new ValidationService(progress).validate(Version.V_1_0, source(code));

        // Then
        assertFalse(result.isCorrect());
        assertTrue(progress.lastReported() < code.length() / 2);
    }

    @Test
    @DisplayName(
            "Given: a progress listener | When: formatting, analyzing and executing | Then: each"
                    + " tool reports the whole source as consumed")
    void services_whenProcessingSource_thenAllReportProgress() {
        // Given
        SpyProgressListener formatting = new SpyProgressListener();
        SpyProgressListener analyzing = new SpyProgressListener();
        SpyProgressListener executing = new SpyProgressListener();
        List<String> printed = new ArrayList<>();

        // When
        new FormatService(formatting).format(Version.V_1_0, source(SOURCE), new StringWriter());
        new LintService(analyzing).analyze(Version.V_1_0, source(SOURCE));
        new ExecuteService(executing)
                .execute(Version.V_1_0, printed::add, prompt -> "", source(SOURCE));

        // Then
        assertEquals(SOURCE.length(), formatting.lastReported());
        assertEquals(SOURCE.length(), analyzing.lastReported());
        assertEquals(SOURCE.length(), executing.lastReported());
        assertEquals(List.of("1"), printed);
    }

    @Test
    @DisplayName(
            "Given: a statement that prints | When: executing | Then: the output is emitted"
                    + " before the following statements are read (streaming execution)")
    void execution_whenPrinting_thenEmitsBeforeReadingTheRest() {
        // Given
        String code = "println(1);\n" + "let x: number = 2;\n".repeat(20);
        SpyProgressListener progress = new SpyProgressListener();
        List<Long> consumedWhenPrinted = new ArrayList<>();

        // When
        new ExecuteService(progress)
                .execute(
                        Version.V_1_0,
                        line -> consumedWhenPrinted.add(progress.lastReported()),
                        prompt -> "",
                        source(code));

        // Then
        assertEquals(1, consumedWhenPrinted.size());
        assertTrue(consumedWhenPrinted.get(0) < code.length() / 2);
    }
}
