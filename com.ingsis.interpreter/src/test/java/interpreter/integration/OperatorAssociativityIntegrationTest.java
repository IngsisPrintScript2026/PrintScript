/*
 * My Project
 */

package interpreter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import result.Result;
import semantic.environment.SemanticEnvironment;
import version.Version;

class OperatorAssociativityIntegrationTest {

    @ParameterizedTest(name = "{0} prints {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "10 - 4 - 3|3",
                "20 / 2 / 5|2",
                "2 * 3 / 6|1",
                "100 / 10 * 2|20",
                "10 - 2 + 3|11",
                "\"Sum: \" + 1 + 2|Sum: 12",
                "1 + 2 + \" apples\"|3 apples",
                "2 + 3 * 4 - 6 / 2|11"
            })
    @DisplayName(
            "Given: a chain of operators of the same precedence | When: executing println of it |"
                    + " Then: they group from the left, as in TypeScript")
    void interpreter_whenChainingSamePrecedence_thenGroupsFromTheLeft(
            String expression, String expected) {
        // Given
        List<String> output = new ArrayList<>();
        PrintScriptRunner runner = new PrintScriptRunner(Version.V_1_0, prompt -> "", name -> null);

        // When
        Result<SemanticEnvironment> result =
                runner.run("println(" + expression + ");", output::add);

        // Then
        assertTrue(result.isCorrect(), result.toString());
        assertEquals(List.of(expected), output);
    }
}
