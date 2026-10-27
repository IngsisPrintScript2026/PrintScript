/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;
import semantic.handler.AssignNodeSemanticHandler;
import semantic.handler.CallFunctionNodeSemanticHandler;
import semantic.handler.DeclarationNodeSemanticHandler;
import semantic.handler.IfNodeSemanticHandler;
import version.Version;

class SemanticHandlersUnitTest {

    private static DeclarationKeywordNode declaration(
            DeclarationType kind, String name, ExpressionNode value, DataType type) {
        return new DeclarationKeywordNode(kind, new IdentifierNode(name, 1, 5), value, type, 1, 1);
    }

    private static AssignNode assign(String name, ExpressionNode value) {
        return new AssignNode(new IdentifierNode(name, 2, 1), value, 2, 1);
    }

    private static IfKeywordNode ifWith(ExpressionNode condition) {
        return new IfKeywordNode(condition, List.of(), List.of(), 4, 1);
    }

    private static StringLiteralNode string(String value) {
        return new StringLiteralNode(value, 1, 1);
    }

    private static String errorOf(Result<SemanticEnvironment> result) {
        assertFalse(result.isCorrect());
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    private static SemanticEnvironment envOf(Result<SemanticEnvironment> result) {
        assertTrue(result.isCorrect());
        return ((CorrectResult<SemanticEnvironment>) result).value();
    }

    @Test
    @DisplayName(
            "Given: 'let x: number;' | When: checking the declaration | Then: x is stored as an"
                    + " uninitialized NUMBER")
    void declarationHandler_whenUninitialized_thenKeepsDeclaredType() {
        // Given
        DeclarationNodeSemanticHandler handler = new DeclarationNodeSemanticHandler();
        DeclarationKeywordNode decl =
                declaration(DeclarationType.LET, "x", new NilExpressionNode(), DataType.NUMBER);

        // When
        SemanticEnvironment env = envOf(handler.check(decl, new SemanticEnvironment()));

        // Then
        SemanticEnvironment.VariableSymbol symbol = env.lookup("x").orElseThrow();
        assertEquals(DataType.NUMBER, symbol.type());
        assertFalse(symbol.isInitialized());
    }

    @Test
    @DisplayName(
            "Given: 'let x: number = \"hola\";' | When: checking | Then: fails with 'Cannot assign"
                    + " type STRING to variable 'x' of type NUMBER'")
    void declarationHandler_whenValueTypeDiffers_thenReportsTypeMismatch() {
        // Given
        DeclarationNodeSemanticHandler handler = new DeclarationNodeSemanticHandler();
        DeclarationKeywordNode decl =
                declaration(DeclarationType.LET, "x", string("hola"), DataType.NUMBER);

        // When
        Result<SemanticEnvironment> result = handler.check(decl, new SemanticEnvironment());

        // Then
        assertEquals("Cannot assign type STRING to variable 'x' of type NUMBER", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: x already declared | When: declaring x again | Then: fails saying it is"
                    + " already declared")
    void declarationHandler_whenRedeclared_thenReportsAlreadyDeclared() {
        // Given
        DeclarationNodeSemanticHandler handler = new DeclarationNodeSemanticHandler();
        SemanticEnvironment env =
                new SemanticEnvironment().define("x", DataType.STRING, true, true);
        DeclarationKeywordNode decl =
                declaration(DeclarationType.LET, "x", string("a"), DataType.STRING);

        // When
        Result<SemanticEnvironment> result = handler.check(decl, env);

        // Then
        assertTrue(errorOf(result).contains("'x' is already declared"));
    }

    @Test
    @DisplayName(
            "Given: 1.0 handler | When: checking 'let s: string = readInput(\"?\");' | Then: fails"
                    + " because readInput is not part of 1.0")
    void declarationHandler_whenInitializerUsesUnsupportedFunction_thenFails() {
        // Given
        DeclarationNodeSemanticHandler handler =
                new DeclarationNodeSemanticHandler(new ExpressionTypeInference(Version.V_1_0));
        CallFunctionNode readInput =
                new CallFunctionNode(
                        new IdentifierNode("readInput", 1, 17), List.of(string("?")), 1, 17);
        DeclarationKeywordNode decl =
                declaration(DeclarationType.LET, "s", readInput, DataType.STRING);

        // When
        Result<SemanticEnvironment> result = handler.check(decl, new SemanticEnvironment());

        // Then
        assertTrue(errorOf(result).contains("not supported in PrintScript 1.0"));
    }

    @Test
    @DisplayName(
            "Given: x declared as number without value | When: assigning \"hola\" to x | Then:"
                    + " fails with a type mismatch for x")
    void assignHandler_whenAssigningWrongTypeToTypedVariable_thenReportsMismatch() {
        // Given
        AssignNodeSemanticHandler handler = new AssignNodeSemanticHandler();
        SemanticEnvironment env =
                new SemanticEnvironment().define("x", DataType.NUMBER, true, false);

        // When
        Result<SemanticEnvironment> result = handler.check(assign("x", string("hola")), env);

        // Then
        assertTrue(errorOf(result).contains("'x'"));
    }

    @Test
    @DisplayName(
            "Given: an initialized const c | When: assigning to c | Then: fails saying a constant"
                    + " cannot be reassigned")
    void assignHandler_whenReassigningConst_thenFails() {
        // Given
        AssignNodeSemanticHandler handler = new AssignNodeSemanticHandler();
        SemanticEnvironment env =
                new SemanticEnvironment().define("c", DataType.NUMBER, false, true);
        NumberLiteralNode two = new NumberLiteralNode(BigDecimal.TWO, 2, 5);

        // When
        Result<SemanticEnvironment> result = handler.check(assign("c", two), env);

        // Then
        assertTrue(errorOf(result).contains("Cannot reassign constant variable 'c'"));
    }

    @Test
    @DisplayName(
            "Given: an empty environment | When: assigning to y | Then: fails saying y is"
                    + " undeclared")
    void assignHandler_whenVariableUndeclared_thenFails() {
        // Given
        AssignNodeSemanticHandler handler = new AssignNodeSemanticHandler();

        // When
        Result<SemanticEnvironment> result =
                handler.check(assign("y", string("a")), new SemanticEnvironment());

        // Then
        assertTrue(errorOf(result).contains("undeclared variable 'y'"));
    }

    @Test
    @DisplayName(
            "Given: n declared as number | When: assigning readInput(\"n?\") | Then: accepted and"
                    + " n keeps type NUMBER")
    void assignHandler_whenAssigningReadInput_thenAdaptsToVariableType() {
        // Given
        AssignNodeSemanticHandler handler =
                new AssignNodeSemanticHandler(new ExpressionTypeInference(Version.V_1_1));
        SemanticEnvironment env =
                new SemanticEnvironment().define("n", DataType.NUMBER, true, false);
        CallFunctionNode readInput =
                new CallFunctionNode(
                        new IdentifierNode("readInput", 2, 5), List.of(string("n?")), 2, 5);

        // When
        SemanticEnvironment updated = envOf(handler.check(assign("n", readInput), env));

        // Then
        assertEquals(DataType.NUMBER, updated.lookup("n").orElseThrow().type());
    }

    @Test
    @DisplayName(
            "Given: an if whose condition is the number 1 | When: checking | Then: fails saying"
                    + " the condition must be boolean")
    void ifHandler_whenConditionIsNumber_thenMustBeBoolean() {
        // Given
        IfNodeSemanticHandler handler = new IfNodeSemanticHandler();
        NumberLiteralNode one = new NumberLiteralNode(BigDecimal.ONE, 4, 5);

        // When
        Result<SemanticEnvironment> result = handler.check(ifWith(one), new SemanticEnvironment());

        // Then
        assertTrue(errorOf(result).contains("must be boolean"));
    }

    @Test
    @DisplayName(
            "Given: an if whose condition is an undeclared variable | When: checking | Then: the"
                    + " undeclared-variable error is reported")
    void ifHandler_whenConditionIsUndeclared_thenReportsInferenceError() {
        // Given
        IfNodeSemanticHandler handler =
                new IfNodeSemanticHandler(new ExpressionTypeInference(Version.V_1_1));

        // When
        Result<SemanticEnvironment> result =
                handler.check(ifWith(new IdentifierNode("flag", 4, 5)), new SemanticEnvironment());

        // Then
        assertTrue(errorOf(result).contains("'flag' is not declared"));
    }

    @Test
    @DisplayName("Given: an if whose condition is 'true' | When: checking | Then: it is accepted")
    void ifHandler_whenConditionIsBoolean_thenAccepted() {
        // Given
        IfNodeSemanticHandler handler = new IfNodeSemanticHandler();

        // When
        Result<SemanticEnvironment> result =
                handler.check(
                        ifWith(new BooleanLiteralNode(true, 4, 5)), new SemanticEnvironment());

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: 1.0 call handler | When: checking readInput(\"x\") | Then: fails as not"
                    + " supported in 1.0")
    void callHandler_whenFunctionNotInVersion_thenFails() {
        // Given
        CallFunctionNodeSemanticHandler handler =
                new CallFunctionNodeSemanticHandler(new ExpressionTypeInference(Version.V_1_0));
        CallFunctionNode call =
                new CallFunctionNode(
                        new IdentifierNode("readInput", 1, 1), List.of(string("x")), 1, 1);

        // When
        Result<SemanticEnvironment> result = handler.check(call, new SemanticEnvironment());

        // Then
        assertEquals("Function 'readInput' is not supported in PrintScript 1.0", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: call handler with default version (1.1) | When: checking readInput(\"x\") |"
                    + " Then: it is accepted")
    void callHandler_whenDefaultVersion_thenAcceptsReadInput() {
        // Given
        CallFunctionNodeSemanticHandler handler = new CallFunctionNodeSemanticHandler();
        CallFunctionNode call =
                new CallFunctionNode(
                        new IdentifierNode("readInput", 1, 1), List.of(string("x")), 1, 1);

        // When
        Result<SemanticEnvironment> result = handler.check(call, new SemanticEnvironment());

        // Then
        assertTrue(result.isCorrect());
    }
}
