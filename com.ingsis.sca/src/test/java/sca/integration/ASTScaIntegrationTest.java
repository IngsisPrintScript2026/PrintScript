/*
 * My Project
 */

package sca.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import node.ProgramNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import sca.ASTSca;
import semantic.environment.SemanticEnvironment;

class ASTScaIntegrationTest {
    private static final String CAMEL = "identifier_format: \"camel case\"\n";
    private static final String SNAKE = "identifier_format: \"snake case\"\n";
    private static final String PRINTLN_RULE = "mandatory-variable-or-literal-in-println: true\n";
    private static final String READ_INPUT_RULE =
            "mandatory-variable-or-literal-in-readInput: true\n";

    private static List<String> violations(String source, String yaml) {
        Result<List<String>> result = LintRunner.lint(source, yaml);
        assertTrue(result.isCorrect(), () -> ((IncorrectResult<List<String>>) result).error());
        return ((CorrectResult<List<String>>) result).value();
    }

    @Test
    @DisplayName(
            "Given: camel case config | When: linting camel case declarations | Then: reports no"
                    + " violation")
    void astSca_whenCamelCaseCodeUnderCamelCase_thenPasses() {
        // Given
        String source = "let myName: string = \"Joe\";\nconst maxValue: number = 3;";

        // When
        List<String> violations = violations(source, CAMEL);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: camel case config | When: line 2 declares 'my_name' | Then: reports the name,"
                    + " the convention and line 2")
    void astSca_whenSnakeCaseNameUnderCamelCase_thenReportsViolationAtItsLine() {
        // Given
        String source = "let okName: number = 1;\nlet my_name: string = \"Joe\";";

        // When
        List<String> violations = violations(source, CAMEL);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'my_name'"));
        assertTrue(violations.get(0).contains("camel case"));
        assertTrue(violations.get(0).contains("line 2"));
    }

    @Test
    @DisplayName(
            "Given: snake case config | When: a const is declared as 'maxValue' | Then: reports"
                    + " the violation")
    void astSca_whenConstViolatesSnakeCase_thenReportsViolation() {
        // Given
        String source = "const maxValue: number = 10;";

        // When
        List<String> violations = violations(source, SNAKE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'maxValue'"));
        assertTrue(violations.get(0).contains("snake case"));
    }

    @Test
    @DisplayName(
            "Given: snake case config | When: linting snake case declarations | Then: reports no"
                    + " violation")
    void astSca_whenSnakeCaseCodeUnderSnakeCase_thenPasses() {
        // Given
        String source = "let my_name: string = \"Joe\";\nlet total_2: number = 2;";

        // When
        List<String> violations = violations(source, SNAKE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println(1 + 2) on line 1 | Then: reports a"
                    + " println argument violation")
    void astSca_whenPrintlnHasExpressionAndRuleOn_thenReportsViolation() {
        // Given
        String source = "println(1 + 2);";

        // When
        List<String> violations = violations(source, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'println'"));
        assertTrue(violations.get(0).contains("line 1"));
    }

    @Test
    @DisplayName("Given: println rule disabled | When: println(1 + 2) | Then: reports no violation")
    void astSca_whenPrintlnHasExpressionAndRuleOff_thenPasses() {
        // Given
        String source = "println(1 + 2);";

        // When
        List<String> violations =
                violations(source, "mandatory-variable-or-literal-in-println: false\n");

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println receives a variable and a literal | Then:"
                    + " reports no violation")
    void astSca_whenPrintlnHasVariableOrLiteral_thenPasses() {
        // Given
        String source = "let x: number = 1 + 2;\nprintln(x);\nprintln(\"done\");";

        // When
        List<String> violations = violations(source, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: readInput rule enabled | When: a declaration uses readInput(\"a\" + \"b\") |"
                    + " Then: reports a readInput violation")
    void astSca_whenReadInputHasExpressionAndRuleOn_thenReportsViolation() {
        // Given
        String source = "let name: string = readInput(\"Your \" + \"name:\");";

        // When
        List<String> violations = violations(source, READ_INPUT_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'readInput'"));
    }

    @Test
    @DisplayName(
            "Given: readInput rule disabled | When: a declaration uses readInput of an expression |"
                    + " Then: reports no violation")
    void astSca_whenReadInputHasExpressionAndRuleOff_thenPasses() {
        // Given
        String source = "let name: string = readInput(\"Your \" + \"name:\");";

        // When
        List<String> violations = violations(source, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: readInput rule enabled | When: readInput receives a literal | Then: reports no"
                    + " violation")
    void astSca_whenReadInputHasLiteral_thenPasses() {
        // Given
        String source = "let name: string = readInput(\"Name:\");";

        // When
        List<String> violations = violations(source, READ_INPUT_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: snake case and println rules | When: violations live inside if and else bodies"
                    + " | Then: both are reported")
    void astSca_whenViolationsInsideIfAndElse_thenReportsBoth() {
        // Given
        String source = "if (true) {\n  let innerName: number = 1;\n} else {\n  println(2 * 3);\n}";

        // When
        List<String> violations = violations(source, SNAKE + PRINTLN_RULE);

        // Then
        assertEquals(2, violations.size());
        assertTrue(violations.get(0).contains("'innerName'"));
        assertTrue(violations.get(0).contains("line 2"));
        assertTrue(violations.get(1).contains("'println'"));
        assertTrue(violations.get(1).contains("line 4"));
    }

    @Test
    @DisplayName(
            "Given: all rules enabled | When: linting a fully compliant program | Then: reports no"
                    + " violation")
    void astSca_whenProgramIsCompliant_thenPasses() {
        // Given
        String source =
                "let user_name: string = readInput(\"Name:\");\n"
                        + "if (true) {\n  println(user_name);\n}";

        // When
        List<String> violations = violations(source, SNAKE + PRINTLN_RULE + READ_INPUT_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a source with a semantic error | When: linting it | Then: fails with a labelled"
                    + " semantic error instead of violations")
    void astSca_whenSourceHasSemanticError_thenFails() {
        // Given
        String source = "let x: number = \"text\";";

        // When
        Result<List<String>> result = LintRunner.lint(source, SNAKE);

        // Then
        assertFalse(result.isCorrect());
        assertTrue(((IncorrectResult<List<String>>) result).error().contains("Semantic error"));
    }

    @Test
    @DisplayName(
            "Given: a whole parsed program and camel case config | When: analyzing the program at"
                    + " once | Then: reports every offending declaration")
    void astSca_whenAnalyzingWholeProgram_thenReportsAllViolations() {
        // Given
        ProgramNode program =
                LintRunner.parseProgram(
                        "let first_bad: number = 1;\nlet goodOne: number = 2;\n"
                                + "let second_bad: number = 3;");
        ASTSca sca = ASTSca.fromYamlConfig(LintRunner.stream(CAMEL));

        // When
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        // Then
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(2, violations.size());
        assertTrue(violations.get(0).contains("line 1"));
        assertTrue(violations.get(1).contains("line 3"));
    }
}
