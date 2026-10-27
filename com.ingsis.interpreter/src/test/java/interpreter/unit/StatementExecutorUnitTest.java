/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.PrintlnFunction;
import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import executor.DefaultStatementExecutor;
import interpreter.fake.FakeFunctionRegistry;
import interpreter.fake.SpyBuiltInFunction;
import interpreter.fake.SpyOutput;
import interpreter.fake.StubExpressionEvaluator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.IncorrectResult;
import result.Result;

class StatementExecutorUnitTest {

    private static DeclarationKeywordNode declare(
            DeclarationType kind, String name, ExpressionNode value, DataType type) {
        return new DeclarationKeywordNode(kind, new IdentifierNode(name, 1, 1), value, type, 1, 1);
    }

    private static AssignNode assign(String name, ExpressionNode value) {
        return new AssignNode(new IdentifierNode(name, 1, 1), value, 1, 1);
    }

    private static CallFunctionNode call(String name, ExpressionNode... args) {
        return new CallFunctionNode(new IdentifierNode(name, 1, 1), List.of(args), 1, 1);
    }

    private static CallFunctionNode println(String text) {
        return call("println", new StringLiteralNode(text, 1, 1));
    }

    private static IfKeywordNode ifNode(
            ExpressionNode condition, List<Node> thenBody, List<Node> elseBody) {
        return new IfKeywordNode(condition, thenBody, elseBody, 1, 1);
    }

    private static NumberLiteralNode num(String value) {
        return new NumberLiteralNode(new BigDecimal(value), 1, 1);
    }

    private static DefaultStatementExecutor stubbed(StubExpressionEvaluator evaluator) {
        return new DefaultStatementExecutor(evaluator, new FakeFunctionRegistry(), new SpyOutput());
    }

    private static DefaultStatementExecutor printing(SpyOutput output) {
        FakeFunctionRegistry registry = new FakeFunctionRegistry(new PrintlnFunction());
        return new DefaultStatementExecutor(
                new DefaultExpressionEvaluator(registry, output), registry, output);
    }

    private static String errorOf(Result<Void> result) {
        assertFalse(result.isCorrect(), () -> "expected failure but was " + result);
        return ((IncorrectResult<Void>) result).error();
    }

    @Test
    @DisplayName(
            "Given: let x: number = <expr evaluating to 5> | When: executing | Then: x holds 5")
    void executor_whenExecutingDeclaration_thenStoresEvaluatedValue() {
        // Given
        StubExpressionEvaluator evaluator =
                new StubExpressionEvaluator(Result.success(new BigDecimal("5")));
        Environment env = new Environment();

        // When
        stubbed(evaluator)
                .execute(declare(DeclarationType.LET, "x", num("0"), DataType.NUMBER), env);

        // Then
        assertEquals(new BigDecimal("5"), env.get("x").value());
    }

    @Test
    @DisplayName(
            "Given: a number declaration | When: executing | Then: the initializer is evaluated"
                    + " with number as target type")
    void executor_whenExecutingDeclaration_thenUsesDeclaredTypeAsTarget() {
        // Given
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.success("41"));

        // When
        stubbed(evaluator)
                .execute(
                        declare(DeclarationType.LET, "x", num("0"), DataType.NUMBER),
                        new Environment());

        // Then
        assertEquals(List.of(DataType.NUMBER), evaluator.targetTypes());
    }

    @Test
    @DisplayName(
            "Given: const c: string = \"v\" | When: executing | Then: c is stored as an immutable"
                    + " string")
    void executor_whenExecutingConstDeclaration_thenStoresTypeAndImmutability() {
        // Given
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.success("v"));
        Environment env = new Environment();

        // When
        stubbed(evaluator)
                .execute(
                        declare(
                                DeclarationType.CONST,
                                "c",
                                new StringLiteralNode("v", 1, 1),
                                DataType.STRING),
                        env);

        // Then
        assertEquals(DataType.STRING, env.get("c").type());
        assertFalse(env.get("c").isMutable());
    }

    @Test
    @DisplayName(
            "Given: a declaration without initializer | When: executing | Then: the variable"
                    + " holds null and nothing is evaluated")
    void executor_whenDeclarationHasNoInitializer_thenStoresNull() {
        // Given
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.success("unused"));
        Environment env = new Environment();

        // When
        Result<Void> result =
                stubbed(evaluator)
                        .execute(declare(DeclarationType.LET, "x", null, DataType.NUMBER), env);

        // Then
        assertTrue(result.isCorrect());
        assertNull(env.get("x").value());
        assertTrue(evaluator.targetTypes().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: an initializer that fails | When: executing the declaration | Then: the"
                    + " evaluation error is returned")
    void executor_whenDeclarationInitializerFails_thenReturnsError() {
        // Given
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.failure("boom"));

        // When
        Result<Void> result =
                stubbed(evaluator)
                        .execute(
                                declare(DeclarationType.LET, "x", num("1"), DataType.NUMBER),
                                new Environment());

        // Then
        assertEquals("boom", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: x already declared | When: declaring x again | Then: fails with 'Declaration"
                    + " error'")
    void executor_whenRedeclaringVariable_thenFails() {
        // Given
        Environment env = new Environment();
        env.declare("x", BigDecimal.ONE, DataType.NUMBER, true);
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.success("v"));

        // When
        Result<Void> result =
                stubbed(evaluator)
                        .execute(declare(DeclarationType.LET, "x", num("2"), DataType.NUMBER), env);

        // Then
        assertTrue(errorOf(result).contains("Declaration error"));
    }

    @Test
    @DisplayName("Given: let x = 1 | When: executing x = <expr evaluating to 9> | Then: x holds 9")
    void executor_whenExecutingAssignment_thenUpdatesValue() {
        // Given
        Environment env = new Environment();
        env.declare("x", BigDecimal.ONE, DataType.NUMBER, true);
        StubExpressionEvaluator evaluator =
                new StubExpressionEvaluator(Result.success(new BigDecimal("9")));

        // When
        stubbed(evaluator).execute(assign("x", num("9")), env);

        // Then
        assertEquals(new BigDecimal("9"), env.get("x").value());
    }

    @Test
    @DisplayName(
            "Given: a boolean variable | When: assigning to it | Then: the expression is evaluated"
                    + " with boolean as target type")
    void executor_whenExecutingAssignment_thenUsesVariableTypeAsTarget() {
        // Given
        Environment env = new Environment();
        env.declare("b", Boolean.FALSE, DataType.BOOLEAN, true);
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.success(true));

        // When
        stubbed(evaluator).execute(assign("b", new BooleanLiteralNode(true, 1, 1)), env);

        // Then
        assertEquals(List.of(DataType.BOOLEAN), evaluator.targetTypes());
    }

    @Test
    @DisplayName(
            "Given: no variable y | When: assigning y | Then: fails with 'Assignment error' and"
                    + " no target type")
    void executor_whenAssigningUndeclaredVariable_thenFails() {
        // Given
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.success("v"));

        // When
        Result<Void> result = stubbed(evaluator).execute(assign("y", num("1")), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Assignment error"));
        assertEquals(Arrays.asList((DataType) null), evaluator.targetTypes());
    }

    @Test
    @DisplayName(
            "Given: a const variable | When: assigning it | Then: fails with 'Cannot reassign"
                    + " constant'")
    void executor_whenAssigningConstant_thenFails() {
        // Given
        Environment env = new Environment();
        env.declare("c", BigDecimal.ONE, DataType.NUMBER, false);
        StubExpressionEvaluator evaluator =
                new StubExpressionEvaluator(Result.success(BigDecimal.TEN));

        // When
        Result<Void> result = stubbed(evaluator).execute(assign("c", num("10")), env);

        // Then
        assertTrue(errorOf(result).contains("Cannot reassign constant variable 'c'"));
    }

    @Test
    @DisplayName(
            "Given: an assigned expression that fails | When: executing | Then: the evaluation"
                    + " error is returned and the value is kept")
    void executor_whenAssignedExpressionFails_thenReturnsError() {
        // Given
        Environment env = new Environment();
        env.declare("x", BigDecimal.ONE, DataType.NUMBER, true);
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.failure("bad"));

        // When
        Result<Void> result = stubbed(evaluator).execute(assign("x", num("2")), env);

        // Then
        assertEquals("bad", errorOf(result));
        assertEquals(BigDecimal.ONE, env.get("x").value());
    }

    @Test
    @DisplayName(
            "Given: if (true) with then and else bodies | When: executing | Then: only the then"
                    + " body runs")
    void executor_whenConditionIsTrue_thenRunsThenBody() {
        // Given
        SpyOutput output = new SpyOutput();
        IfKeywordNode statement =
                ifNode(
                        new BooleanLiteralNode(true, 1, 1),
                        List.of(println("then")),
                        List.of(println("else")));

        // When
        printing(output).execute(statement, new Environment());

        // Then
        assertEquals(List.of("then"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: if (false) with then and else bodies | When: executing | Then: only the else"
                    + " body runs")
    void executor_whenConditionIsFalse_thenRunsElseBody() {
        // Given
        SpyOutput output = new SpyOutput();
        IfKeywordNode statement =
                ifNode(
                        new BooleanLiteralNode(false, 1, 1),
                        List.of(println("then")),
                        List.of(println("else")));

        // When
        printing(output).execute(statement, new Environment());

        // Then
        assertEquals(List.of("else"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: if with a number condition | When: executing | Then: fails because the"
                    + " condition must be boolean")
    void executor_whenConditionIsNotBoolean_thenFails() {
        // Given
        IfKeywordNode statement = ifNode(num("1"), List.of(), List.of());

        // When
        Result<Void> result = printing(new SpyOutput()).execute(statement, new Environment());

        // Then
        assertTrue(errorOf(result).contains("must evaluate to a boolean"));
    }

    @Test
    @DisplayName(
            "Given: if whose condition cannot be evaluated | When: executing | Then: the"
                    + " evaluation error is returned")
    void executor_whenConditionFails_thenReturnsError() {
        // Given
        StubExpressionEvaluator evaluator = new StubExpressionEvaluator(Result.failure("cond"));
        IfKeywordNode statement = ifNode(num("1"), List.of(), List.of());

        // When
        Result<Void> result = stubbed(evaluator).execute(statement, new Environment());

        // Then
        assertEquals("cond", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a declaration inside an if body | When: executing | Then: the variable does"
                    + " not leak to the outer scope")
    void executor_whenDeclaringInsideIf_thenVariableIsScopedToBlock() {
        // Given
        Environment env = new Environment();
        IfKeywordNode statement =
                ifNode(
                        new BooleanLiteralNode(true, 1, 1),
                        List.of(declare(DeclarationType.LET, "inner", num("1"), DataType.NUMBER)),
                        List.of());

        // When
        printing(new SpyOutput()).execute(statement, env);

        // Then
        assertFalse(env.find("inner").isPresent());
    }

    @Test
    @DisplayName(
            "Given: an if body whose first statement fails | When: executing | Then: the error is"
                    + " returned and later statements do not run")
    void executor_whenStatementInsideIfFails_thenStopsAndReturnsError() {
        // Given
        SpyOutput output = new SpyOutput();
        IfKeywordNode statement =
                ifNode(
                        new BooleanLiteralNode(true, 1, 1),
                        List.of(assign("ghost", num("1")), println("after")),
                        List.of());

        // When
        Result<Void> result = printing(output).execute(statement, new Environment());

        // Then
        assertTrue(errorOf(result).contains("ghost"));
        assertTrue(output.lines().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a registered function | When: executing a call statement | Then: it receives"
                    + " the evaluated arguments")
    void executor_whenExecutingCall_thenPassesEvaluatedArguments() {
        // Given
        SpyBuiltInFunction shout = new SpyBuiltInFunction("shout");
        FakeFunctionRegistry registry = new FakeFunctionRegistry(shout);
        DefaultStatementExecutor executor =
                new DefaultStatementExecutor(
                        new DefaultExpressionEvaluator(registry, null), registry, null);

        // When
        executor.execute(call("shout", num("1"), num("2")), new Environment());

        // Then
        assertEquals(List.of(List.of(BigDecimal.ONE, new BigDecimal("2"))), shout.calls());
    }

    @Test
    @DisplayName(
            "Given: no function 'foo' | When: executing foo() | Then: fails with 'Undefined"
                    + " function'")
    void executor_whenCallingUndefinedFunction_thenFails() {
        // Given
        DefaultStatementExecutor executor = printing(new SpyOutput());

        // When
        Result<Void> result = executor.execute(call("foo"), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Undefined function: foo"));
    }

    @Test
    @DisplayName(
            "Given: a call whose argument fails | When: executing | Then: the argument error is"
                    + " returned and the function is not run")
    void executor_whenCallArgumentFails_thenReturnsError() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        Result<Void> result =
                printing(output)
                        .execute(
                                call("println", new IdentifierNode("ghost", 1, 1)),
                                new Environment());

        // Then
        assertTrue(errorOf(result).contains("ghost"));
        assertTrue(output.lines().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a function that throws | When: executing its call | Then: fails with"
                    + " 'Function execution error'")
    void executor_whenFunctionThrows_thenFails() {
        // Given
        FakeFunctionRegistry registry =
                new FakeFunctionRegistry(new SpyBuiltInFunction("explode", "kaboom"));
        DefaultStatementExecutor executor =
                new DefaultStatementExecutor(
                        new DefaultExpressionEvaluator(registry, null), registry, null);

        // When
        Result<Void> result = executor.execute(call("explode"), new Environment());

        // Then
        String error = errorOf(result);
        assertTrue(error.contains("Function execution error"));
        assertTrue(error.contains("kaboom"));
    }

    @Test
    @DisplayName(
            "Given: a program node as statement | When: executing | Then: fails with 'Unsupported"
                    + " statement node'")
    void executor_whenStatementIsUnsupported_thenFails() {
        // Given
        DefaultStatementExecutor executor = printing(new SpyOutput());

        // When
        Result<Void> result = executor.execute(new ProgramNode(List.of(), 1, 1), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Unsupported statement node"));
    }

    @Test
    @DisplayName(
            "Given: an executor built only from an output emitter | When: executing println |"
                    + " Then: the default registry prints the line")
    void executor_whenBuiltWithDefaultRegistry_thenPrints() {
        // Given
        SpyOutput output = new SpyOutput();
        DefaultStatementExecutor executor = new DefaultStatementExecutor(output);

        // When
        executor.execute(println("hi"), new Environment());

        // Then
        assertEquals(List.of("hi"), output.lines());
    }
}
