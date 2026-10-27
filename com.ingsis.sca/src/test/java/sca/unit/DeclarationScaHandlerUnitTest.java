/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sca.fake.ScaNodes.call;
import static sca.fake.ScaNodes.declare;
import static sca.fake.ScaNodes.let;
import static sca.fake.ScaNodes.sumAt;
import static sca.fake.ScaNodes.text;

import java.util.List;
import node.keyword.DeclarationKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sca.ASTSca;
import sca.ScaContext;
import sca.handler.DeclarationScaHandler;
import semantic.environment.SemanticEnvironment;

class DeclarationScaHandlerUnitTest {

    private static List<String> check(DeclarationKeywordNode decl, ScaContext context) {
        return new DeclarationScaHandler()
                .check(decl, new SemanticEnvironment(), context, new ASTSca(context));
    }

    private static ScaContext naming(String format) {
        return new ScaContext(format, false, false);
    }

    @Test
    @DisplayName(
            "Given: camel case convention | When: declaring 'myVariable2' | Then: reports no"
                    + " violation")
    void declarationHandler_whenCamelCaseNameUnderCamelCase_thenNoViolation() {
        // Given
        DeclarationKeywordNode decl = let("myVariable2", 1, 5);

        // When
        List<String> violations = check(decl, naming("camel case"));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: camel case convention | When: declaring 'my_variable' at line 3, column 5 |"
                    + " Then: reports the name, the convention and the position")
    void declarationHandler_whenSnakeCaseNameUnderCamelCase_thenReportsViolationWithPosition() {
        // Given
        DeclarationKeywordNode decl = let("my_variable", 3, 5);

        // When
        List<String> violations = check(decl, naming("camel case"));

        // Then
        assertEquals(1, violations.size());
        String violation = violations.get(0);
        assertTrue(violation.contains("'my_variable'"));
        assertTrue(violation.contains("camel case"));
        assertTrue(violation.contains("line 3, column 5"));
    }

    @Test
    @DisplayName(
            "Given: camel case convention | When: declaring a name starting with uppercase | Then:"
                    + " reports a violation")
    void declarationHandler_whenPascalCaseNameUnderCamelCase_thenReportsViolation() {
        // Given
        DeclarationKeywordNode decl = let("MyVariable", 1, 5);

        // When
        List<String> violations = check(decl, naming("camel case"));

        // Then
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName(
            "Given: snake case convention | When: declaring 'my_variable_2' | Then: reports no"
                    + " violation")
    void declarationHandler_whenSnakeCaseNameUnderSnakeCase_thenNoViolation() {
        // Given
        DeclarationKeywordNode decl = let("my_variable_2", 1, 5);

        // When
        List<String> violations = check(decl, naming("snake case"));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: snake case convention | When: declaring 'myVariable' at line 2, column 7 |"
                    + " Then: reports the name, the convention and the position")
    void declarationHandler_whenCamelCaseNameUnderSnakeCase_thenReportsViolationWithPosition() {
        // Given
        DeclarationKeywordNode decl = let("myVariable", 2, 7);

        // When
        List<String> violations = check(decl, naming("snake case"));

        // Then
        assertEquals(1, violations.size());
        String violation = violations.get(0);
        assertTrue(violation.contains("'myVariable'"));
        assertTrue(violation.contains("snake case"));
        assertTrue(violation.contains("line 2, column 7"));
    }

    @Test
    @DisplayName(
            "Given: the compact spellings 'camelcase' and 'snake_case' | When: checking a"
                    + " violating name | Then: both are recognised as conventions")
    void declarationHandler_whenCompactConventionSpellings_thenAreRecognised() {
        // Given
        DeclarationKeywordNode decl = let("bad_Name", 1, 5);

        // When
        List<String> camel = check(decl, naming("camelcase"));
        List<String> snake = check(decl, naming("snake_case"));

        // Then
        assertEquals(1, camel.size());
        assertEquals(1, snake.size());
    }

    @Test
    @DisplayName(
            "Given: a convention written with uppercase and surrounding spaces | When: checking a"
                    + " violating name | Then: it is still applied")
    void declarationHandler_whenConventionHasCaseAndSpaces_thenIsNormalised() {
        // Given
        DeclarationKeywordNode decl = let("my_variable", 1, 5);

        // When
        List<String> violations = check(decl, naming("  Camel Case "));

        // Then
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName(
            "Given: an unknown naming convention | When: declaring any name | Then: reports no"
                    + " violation")
    void declarationHandler_whenConventionIsUnknown_thenAcceptsEverything() {
        // Given
        DeclarationKeywordNode decl = let("Whatever_Name", 1, 5);

        // When
        List<String> violations = check(decl, naming("kebab case"));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: no or blank naming convention | When: declaring any name | Then: reports no"
                    + " violation")
    void declarationHandler_whenConventionIsMissing_thenAcceptsEverything() {
        // Given
        DeclarationKeywordNode decl = let("Whatever_Name", 1, 5);

        // When
        List<String> withNull = check(decl, naming(null));
        List<String> withBlank = check(decl, naming("   "));

        // Then
        assertTrue(withNull.isEmpty());
        assertTrue(withBlank.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: readInput rule enabled | When: a declaration is initialised with readInput of"
                    + " an expression | Then: reports the readInput argument violation")
    void declarationHandler_whenInitializerIsReadInputWithExpression_thenReportsViolation() {
        // Given
        DeclarationKeywordNode decl = declare("name", call("readInput", sumAt(1, 30)));
        ScaContext context = new ScaContext(null, false, true);

        // When
        List<String> violations = check(decl, context);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'readInput'"));
    }

    @Test
    @DisplayName(
            "Given: readInput rule disabled | When: a declaration is initialised with readInput of"
                    + " an expression | Then: reports no violation")
    void declarationHandler_whenReadInputRuleDisabled_thenNoViolationForInitializer() {
        // Given
        DeclarationKeywordNode decl = declare("name", call("readInput", sumAt(1, 30)));

        // When
        List<String> violations = check(decl, new ScaContext());

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: both argument rules enabled | When: a declaration is initialised with a plain"
                    + " expression | Then: reports no violation")
    void declarationHandler_whenInitializerIsPlainExpression_thenNoViolation() {
        // Given
        DeclarationKeywordNode decl = declare("total", sumAt(1, 15));
        ScaContext context = new ScaContext(null, true, true);

        // When
        List<String> violations = check(decl, context);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a declaration without initializer | When: checking it | Then: only the name is"
                    + " validated")
    void declarationHandler_whenNoInitializer_thenOnlyNameIsChecked() {
        // Given
        DeclarationKeywordNode decl = declare("ok_name", null);

        // When
        List<String> violations = check(decl, naming("snake case"));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a readInput initializer with a literal argument | When: readInput rule is"
                    + " enabled | Then: reports no violation")
    void declarationHandler_whenReadInputArgumentIsLiteral_thenNoViolation() {
        // Given
        DeclarationKeywordNode decl = declare("name", call("readInput", text("Name:")));

        // When
        List<String> violations = check(decl, new ScaContext(null, false, true));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: DeclarationScaHandler | When: asking its node type | Then: it handles"
                    + " declarations")
    void declarationHandler_whenAskingNodeType_thenIsDeclaration() {
        // Given
        DeclarationScaHandler handler = new DeclarationScaHandler();

        // When
        Class<DeclarationKeywordNode> type = handler.nodeType();

        // Then
        assertEquals(DeclarationKeywordNode.class, type);
    }
}
