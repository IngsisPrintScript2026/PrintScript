/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import environment.Environment;
import executor.DefaultStatementExecutor;
import interpreter.DefaultInterpreter;
import interpreter.fake.FakeEnvProvider;
import interpreter.fake.FakeInputProvider;
import interpreter.fake.FakeStatementExecutor;
import interpreter.fake.SpyOutput;
import interpreter.fake.StubExpressionEvaluator;
import interpreter.fake.StubSemanticChecker;
import interpreter.fake.StubStatementParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import node.Node;
import node.ProgramNode;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.DataType;
import node.expression.literal.StringLiteralNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;
import tokenstream.TokenStreamAdapter;

class DefaultInterpreterUnitTest {

    private static CallFunctionNode println(ExpressionNode argument) {
        return new CallFunctionNode(new IdentifierNode("println", 1, 1), List.of(argument), 1, 1);
    }

    private static CallFunctionNode println(String text) {
        return println(new StringLiteralNode(text, 1, 1));
    }

    private static ProgramNode program(Node... statements) {
        return new ProgramNode(List.of(statements), 1, 1);
    }

    /** One placeholder token per statement, each on its own line. */
    private static TokenStream tokens(int count) {
        List<Token> tokens = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            tokens.add(new Token(TokenType.IDENTIFIER, "t" + i, new Position(i, 1)));
        }
        return new TokenStreamAdapter(tokens, 0);
    }

    private static <T> String errorOf(Result<T> result) {
        assertFalse(result.isCorrect(), () -> "expected failure but was " + result);
        return ((IncorrectResult<T>) result).error();
    }

    @Test
    @DisplayName(
            "Given: a program with two statements | When: interpreting it | Then: both are"
                    + " executed in order")
    void interpreter_whenInterpretingProgram_thenExecutesEveryStatementInOrder() {
        // Given
        FakeStatementExecutor executor = FakeStatementExecutor.succeeding();
        Node first = println("a");
        Node second = println("b");

        // When
        Result<Void> result =
                new DefaultInterpreter(null, null, executor).interpret(program(first, second));

        // Then
        assertTrue(result.isCorrect());
        assertEquals(List.of(first, second), executor.executed());
    }

    @Test
    @DisplayName(
            "Given: an executor that fails | When: interpreting a program | Then: the error is"
                    + " prefixed with 'Runtime error:'")
    void interpreter_whenProgramStatementFails_thenPrefixesRuntimeError() {
        // Given
        FakeStatementExecutor executor = FakeStatementExecutor.failingWith("Division by zero");

        // When
        Result<Void> result =
                new DefaultInterpreter(null, null, executor).interpret(program(println("x")));

        // Then
        assertEquals("Runtime error: Division by zero", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: an error already prefixed with 'Runtime error:' | When: interpreting | Then:"
                    + " the prefix is not duplicated")
    void interpreter_whenErrorAlreadyPrefixed_thenKeepsSinglePrefix() {
        // Given
        FakeStatementExecutor executor = FakeStatementExecutor.failingWith("Runtime error: oops");

        // When
        Result<Void> result =
                new DefaultInterpreter(null, null, executor).interpret(program(println("x")));

        // Then
        assertEquals("Runtime error: oops", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: the first statement fails | When: interpreting a program | Then: the next"
                    + " statements are not executed")
    void interpreter_whenStatementFails_thenStopsExecution() {
        // Given
        FakeStatementExecutor executor = FakeStatementExecutor.failingWith("bad");

        // When
        new DefaultInterpreter(null, null, executor).interpret(program(println("a"), println("b")));

        // Then
        assertEquals(1, executor.executed().size());
    }

    @Test
    @DisplayName(
            "Given: a global environment | When: interpreting a declaration | Then: the variable"
                    + " ends up in that environment")
    void interpreter_whenInterpretingWithEnvironment_thenUsesIt() {
        // Given
        Environment env = new Environment();
        DeclarationKeywordNode declaration =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("x", 1, 1),
                        new StringLiteralNode("v", 1, 1),
                        DataType.STRING,
                        1,
                        1);

        // When
        new DefaultInterpreter(new SpyOutput()).interpret(program(declaration), env);

        // Then
        assertEquals("v", env.get("x").value());
    }

    @Test
    @DisplayName(
            "Given: no statement parser | When: interpreting a token stream | Then: fails asking"
                    + " for the parser dependency")
    void interpreter_whenParserIsMissing_thenFails() {
        // Given
        DefaultInterpreter interpreter =
                new DefaultInterpreter(null, null, FakeStatementExecutor.succeeding());

        // When
        Result<SemanticEnvironment> result =
                interpreter.interpret(tokens(1), new SemanticEnvironment(), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Syntactic parser dependency must be injected"));
    }

    @Test
    @DisplayName(
            "Given: a parser that reports EOF | When: interpreting a token stream | Then: ends"
                    + " successfully")
    void interpreter_whenParserReportsEof_thenSucceeds() {
        // Given
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        StubStatementParser.failingWith("EOF"),
                        StubSemanticChecker.accepting(),
                        FakeStatementExecutor.succeeding());

        // When
        Result<SemanticEnvironment> result =
                interpreter.interpret(tokens(1), new SemanticEnvironment(), new Environment());

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: a parser that fails | When: interpreting a token stream | Then: reports a"
                    + " syntactic error with the statement start position")
    void interpreter_whenParserFails_thenReportsSyntacticErrorWithPosition() {
        // Given
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        StubStatementParser.failingWith("Unexpected token"),
                        StubSemanticChecker.accepting(),
                        FakeStatementExecutor.succeeding());

        // When
        Result<SemanticEnvironment> result =
                interpreter.interpret(tokens(1), new SemanticEnvironment(), new Environment());

        // Then
        String error = errorOf(result);
        assertTrue(error.startsWith("Syntactic error: Unexpected token"));
        assertTrue(error.contains("[in statement starting at line 1, column 1]"));
    }

    @Test
    @DisplayName(
            "Given: a checker that rejects the statement | When: interpreting | Then: reports a"
                    + " semantic error and does not execute it")
    void interpreter_whenCheckerRejects_thenReportsSemanticError() {
        // Given
        FakeStatementExecutor executor = FakeStatementExecutor.succeeding();
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        StubStatementParser.returning(println("x")),
                        StubSemanticChecker.rejectingWith("Type mismatch"),
                        executor);

        // When
        Result<SemanticEnvironment> result =
                interpreter.interpret(tokens(1), new SemanticEnvironment(), new Environment());

        // Then
        String error = errorOf(result);
        assertTrue(error.startsWith("Semantic error: Type mismatch"));
        assertTrue(error.contains("[from line 1, column 1"));
        assertTrue(executor.executed().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: an executor that fails | When: interpreting a token stream | Then: reports a"
                    + " runtime error with the statement range")
    void interpreter_whenExecutorFails_thenReportsRuntimeErrorWithRange() {
        // Given
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        StubStatementParser.returning(println("x")),
                        StubSemanticChecker.accepting(),
                        FakeStatementExecutor.failingWith("Division by zero"));

        // When
        Result<SemanticEnvironment> result =
                interpreter.interpret(tokens(1), new SemanticEnvironment(), new Environment());

        // Then
        String error = errorOf(result);
        assertTrue(error.startsWith("Runtime error: Division by zero"));
        assertTrue(error.contains("[from line 1, column 1 to line 1, column 1]"));
    }

    @Test
    @DisplayName(
            "Given: a stream with two statements | When: interpreting | Then: each statement is"
                    + " executed against the runtime environment")
    void interpreter_whenStreamHasStatements_thenExecutesEachOne() {
        // Given
        SpyOutput output = new SpyOutput();
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        StubStatementParser.returning(println("a"), println("b")),
                        StubSemanticChecker.accepting(),
                        new DefaultStatementExecutor(output));

        // When
        Result<SemanticEnvironment> result =
                interpreter.interpret(tokens(2), new SemanticEnvironment(), new Environment());

        // Then
        assertTrue(result.isCorrect());
        assertEquals(List.of("a", "b"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: an accepted stream | When: interpreting | Then: returns the semantic"
                    + " environment produced by the checker")
    void interpreter_whenStreamSucceeds_thenReturnsSemanticEnvironment() {
        // Given
        SemanticEnvironment semanticEnv = new SemanticEnvironment();
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        StubStatementParser.returning(println("a")),
                        StubSemanticChecker.accepting(),
                        FakeStatementExecutor.succeeding());

        // When
        Result<SemanticEnvironment> result =
                interpreter.interpret(tokens(1), semanticEnv, new Environment());

        // Then
        assertSame(semanticEnv, ((result.CorrectResult<SemanticEnvironment>) result).value());
    }

    @Test
    @DisplayName(
            "Given: an interpreter built from checker and executor | When: interpreting a program"
                    + " | Then: delegates to that executor")
    void interpreter_whenBuiltFromCheckerAndExecutor_thenDelegates() {
        // Given
        FakeStatementExecutor executor = FakeStatementExecutor.succeeding();
        DefaultInterpreter interpreter = new DefaultInterpreter(new SemanticChecker(), executor);

        // When
        interpreter.interpret(program(println("a")));

        // Then
        assertEquals(1, executor.executed().size());
    }

    @Test
    @DisplayName(
            "Given: an interpreter built with a custom evaluator | When: printing | Then: prints"
                    + " the evaluator's value")
    void interpreter_whenBuiltWithEvaluator_thenUsesIt() {
        // Given
        SpyOutput output = new SpyOutput();
        StubExpressionEvaluator evaluator =
                new StubExpressionEvaluator(Result.success(new BigDecimal("7")));
        DefaultInterpreter interpreter =
                new DefaultInterpreter(new SemanticChecker(), evaluator, output);

        // When
        interpreter.interpret(program(println("ignored")));

        // Then
        assertEquals(List.of("7"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: an interpreter built with input and env providers | When: declaring from"
                    + " readEnv | Then: the provider value is stored")
    void interpreter_whenBuiltWithProviders_thenUsesThem() {
        // Given
        Environment env = new Environment();
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        new SemanticChecker(),
                        new SpyOutput(),
                        new FakeInputProvider(),
                        new FakeEnvProvider(Map.of("HOME_DIR", "/tmp")));
        CallFunctionNode readEnv =
                new CallFunctionNode(
                        new IdentifierNode("readEnv", 1, 1),
                        List.of(new StringLiteralNode("HOME_DIR", 1, 1)),
                        1,
                        1);
        DeclarationKeywordNode declaration =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("home", 1, 1),
                        readEnv,
                        DataType.STRING,
                        1,
                        1);

        // When
        interpreter.interpret(program(declaration), env);

        // Then
        assertEquals("/tmp", env.get("home").value());
    }

    @Test
    @DisplayName(
            "Given: the no-arg interpreter | When: interpreting an empty program | Then: succeeds")
    void interpreter_whenDefaultConstructed_thenInterpretsEmptyProgram() {
        // Given
        DefaultInterpreter interpreter = new DefaultInterpreter();

        // When
        Result<Void> result = interpreter.interpret(program());

        // Then
        assertTrue(result.isCorrect());
    }
}
