/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import parser.support.Sources;
import pipeline.StatementAction;
import pipeline.StatementPipeline;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import version.Version;

class LexicalErrorIntegrationTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "at the start of the program|@|line 1, column 1",
                "at the start of a later statement|let x: number = 5; @|line 1, column 20",
                "inside an expression|let x: number = 5 @;|line 1, column 19"
            })
    @DisplayName(
            "Given: an unrecognized character | When: running the statement pipeline | Then: it"
                    + " fails as a lexical error with the character position")
    void pipeline_whenSourceHasUnknownChar_thenReportsLexicalError(
            String where, String source, String position) {
        // Given
        StatementPipeline pipeline = new StatementPipeline(Version.V_1_0);

        // When
        Result<SemanticEnvironment> result =
                pipeline.run(
                        Sources.tokens(source), new SemanticEnvironment(), StatementAction.NONE);

        // Then
        assertFalse(result.isCorrect(), where + " should fail");
        String error = ((IncorrectResult<SemanticEnvironment>) result).error();
        assertTrue(error.startsWith("Lexical error"), error);
        assertTrue(error.contains("'@'") && error.contains(position), error);
    }
}
