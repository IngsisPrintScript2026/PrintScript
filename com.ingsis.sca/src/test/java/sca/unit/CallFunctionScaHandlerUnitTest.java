/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sca.fake.ScaNodes.call;
import static sca.fake.ScaNodes.number;
import static sca.fake.ScaNodes.sumAt;
import static sca.fake.ScaNodes.text;
import static sca.fake.ScaNodes.variable;

import java.util.List;
import node.expression.function.CallFunctionNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sca.ASTSca;
import sca.ScaContext;
import sca.handler.CallFunctionScaHandler;
import semantic.environment.SemanticEnvironment;

class CallFunctionScaHandlerUnitTest {
    private static final ScaContext PRINTLN_RULE = new ScaContext(null, true, false);
    private static final ScaContext READ_INPUT_RULE = new ScaContext(null, false, true);

    private static List<String> check(CallFunctionNode call, ScaContext context) {
        return new CallFunctionScaHandler()
                .check(call, new SemanticEnvironment(), context, new ASTSca(context));
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println receives 1 + 2 at line 4, column 9 |"
                    + " Then: reports the function, the index and the position")
    void callHandler_whenPrintlnHasExpressionAndRuleOn_thenReportsViolation() {
        // Given
        CallFunctionNode println = call("println", sumAt(4, 9));

        // When
        List<String> violations = check(println, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
        String violation = violations.get(0);
        assertTrue(violation.contains("'println'"));
        assertTrue(violation.contains("index 0"));
        assertTrue(violation.contains("line 4, column 9"));
    }

    @Test
    @DisplayName(
            "Given: println rule disabled | When: println receives an expression | Then: reports"
                    + " no violation")
    void callHandler_whenPrintlnHasExpressionAndRuleOff_thenNoViolation() {
        // Given
        CallFunctionNode println = call("println", sumAt(1, 9));

        // When
        List<String> violations = check(println, new ScaContext());

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println receives a literal | Then: reports no"
                    + " violation")
    void callHandler_whenPrintlnHasLiteral_thenNoViolation() {
        // Given
        CallFunctionNode println = call("println", text("hello"));

        // When
        List<String> violations = check(println, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println receives a variable | Then: reports no"
                    + " violation")
    void callHandler_whenPrintlnHasVariable_thenNoViolation() {
        // Given
        CallFunctionNode println = call("println", variable("name"));

        // When
        List<String> violations = check(println, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println receives another function call | Then:"
                    + " reports a violation")
    void callHandler_whenPrintlnHasNestedCall_thenReportsViolation() {
        // Given
        CallFunctionNode println = call("println", call("readInput", text("x")));

        // When
        List<String> violations = check(println, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: the second argument is an expression | Then: the"
                    + " violation reports index 1")
    void callHandler_whenSecondArgumentIsExpression_thenReportsIndexOne() {
        // Given
        CallFunctionNode println = call("println", number(1), sumAt(1, 12));

        // When
        List<String> violations = check(println, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("index 1"));
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: the function name has different casing | Then:"
                    + " the rule still applies")
    void callHandler_whenPrintlnNameHasDifferentCase_thenRuleStillApplies() {
        // Given
        CallFunctionNode println = call("PrintLn", sumAt(1, 9));

        // When
        List<String> violations = check(println, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName(
            "Given: readInput rule enabled | When: readInput receives an expression | Then:"
                    + " reports a readInput violation")
    void callHandler_whenReadInputHasExpressionAndRuleOn_thenReportsViolation() {
        // Given
        CallFunctionNode readInput = call("readInput", sumAt(2, 20));

        // When
        List<String> violations = check(readInput, READ_INPUT_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'readInput'"));
        assertTrue(violations.get(0).contains("line 2, column 20"));
    }

    @Test
    @DisplayName(
            "Given: readInput rule disabled | When: readInput receives an expression | Then:"
                    + " reports no violation")
    void callHandler_whenReadInputHasExpressionAndRuleOff_thenNoViolation() {
        // Given
        CallFunctionNode readInput = call("readInput", sumAt(2, 20));

        // When
        List<String> violations = check(readInput, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: only the readInput rule enabled | When: println receives an expression | Then:"
                    + " reports no violation")
    void callHandler_whenOnlyReadInputRuleOn_thenPrintlnIsNotChecked() {
        // Given
        CallFunctionNode println = call("println", sumAt(1, 9));

        // When
        List<String> violations = check(println, READ_INPUT_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: both rules enabled | When: another function receives an expression | Then:"
                    + " reports no violation")
    void callHandler_whenOtherFunctionHasExpression_thenNoViolation() {
        // Given
        CallFunctionNode readEnv = call("readEnv", sumAt(1, 9));

        // When
        List<String> violations = check(readEnv, new ScaContext(null, true, true));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: CallFunctionScaHandler | When: asking its node type | Then: it handles"
                    + " function calls")
    void callHandler_whenAskingNodeType_thenIsCallFunction() {
        // Given
        CallFunctionScaHandler handler = new CallFunctionScaHandler();

        // When
        Class<CallFunctionNode> type = handler.nodeType();

        // Then
        assertEquals(CallFunctionNode.class, type);
    }
}
