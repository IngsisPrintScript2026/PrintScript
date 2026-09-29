/*
 * My Project
 */

package interpreter.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import executor.DefaultStatementExecutor;
import executor.StatementExecutor;
import interpreter.DefaultInterpreter;
import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.DataType;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

class InterpreterStreamIntegrationTest {

    private static class FakeTokenStream implements TokenStream {
        private final boolean empty;

        public FakeTokenStream(boolean empty) {
            this.empty = empty;
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public int pointer() {
            return 0;
        }

        @Override
        public Result<IterationStep<Token>> consume() {
            return Result.failure("EOF");
        }

        @Override
        public Result<IterationStep<Token>> consume(TokenType expectedType) {
            return Result.failure("EOF");
        }

        @Override
        public Result<IterationStep<Token>> consume(java.util.function.Predicate<Token> matcher) {
            return Result.failure("EOF");
        }

        @Override
        public Result<Token> peek(int offset) {
            return Result.failure("EOF");
        }

        @Override
        public Result<IterationStep<Token>> next() {
            return Result.failure("EOF");
        }
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter constructors | When: initializing with diverse configurations"
                    + " | Then: instantiates successfully")
    void
            defaultInterpreter_whenConstructedWithDiverseConfigurations_thenInstantiatesSuccessfully() {
        // What & When & Then: constructors without mocks
        assertNotNull(new DefaultInterpreter());
        assertNotNull(new DefaultInterpreter(System.out::println));
        assertNotNull(new DefaultInterpreter(new SemanticChecker(), System.out::println));
        assertNotNull(
                new DefaultInterpreter(
                        new SemanticChecker(), System.out::println, prompt -> "", key -> ""));
        assertNotNull(
                new DefaultInterpreter(
                        new SemanticChecker(),
                        new DefaultExpressionEvaluator(),
                        System.out::println));
        assertNotNull(
                new DefaultInterpreter(
                        new SemanticChecker(), new DefaultStatementExecutor(System.out::println)));
        assertNotNull(
                new DefaultInterpreter(
                        null,
                        new SemanticChecker(),
                        System.out::println,
                        new builtin.DefaultFunctionRegistry(prompt -> "", key -> "")));
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter stream interpretation | When: syntactic parser is null |"
                    + " Then: fails with dependency error")
    void interpreterStream_whenNullParserSupplied_thenFailsWithDependencyError() {
        // What: DefaultInterpreter with null syntactic parser
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        null,
                        new SemanticChecker(),
                        new DefaultStatementExecutor(System.out::println));

        // When: interpreting stream
        Result<SemanticEnvironment> result =
                interpreter.interpret(
                        new FakeTokenStream(false), new SemanticEnvironment(), new Environment());

        // Then: fails with dependency injection message
        assertFalse(result.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) result)
                        .error()
                        .contains("Syntactic parser dependency must be injected"));
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter stream interpretation | When: streaming valid statement |"
                    + " Then: executes pipeline and updates SemanticEnvironment")
    void interpreterStream_whenStreamingValidTokens_thenIntegratesParserCheckerAndExecutor() {
        // What: declaration node, FakeTokenStream, and fake collaborators
        Node node1 =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("a", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        TokenStream endStream = new FakeTokenStream(true);
        TokenStream startStream = new FakeTokenStream(false);

        syntactic.Parser<Node> parser =
                stream -> Result.success(new IterationStep<>(node1, endStream));
        SemanticChecker checker =
                new SemanticChecker() {
                    @Override
                    public Result<SemanticEnvironment> checkNode(
                            Node node, SemanticEnvironment env) {
                        return Result.success(env);
                    }
                };
        StatementExecutor executor = (statement, env) -> Result.success(null);

        DefaultInterpreter interpreter = new DefaultInterpreter(parser, checker, executor);

        // When: interpreting start stream
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());

        // Then: succeeds
        assertTrue(res.isCorrect());
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter stream interpretation | When: stream reaches EOF | Then:"
                    + " terminates successfully")
    void interpreterStream_whenTokenStreamReachesEof_thenTerminatesSuccessfully() {
        // What: FakeTokenStream that returns EOF failure from parser
        TokenStream startStream = new FakeTokenStream(false);
        syntactic.Parser<Node> parser = stream -> Result.failure("EOF");
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        parser,
                        new SemanticChecker(),
                        new DefaultStatementExecutor(System.out::println));

        // When: interpreting EOF stream
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());

        // Then: succeeds gracefully
        assertTrue(res.isCorrect());
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter stream interpretation | When: syntactic error occurs | Then:"
                    + " returns failure with syntactic error message")
    void interpreterStream_whenSyntacticErrorOccursInStream_thenReturnsSyntacticError() {
        // What: parser that fails with syntactic error
        TokenStream startStream = new FakeTokenStream(false);
        syntactic.Parser<Node> parser = stream -> Result.failure("Unexpected token");
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        parser,
                        new SemanticChecker(),
                        new DefaultStatementExecutor(System.out::println));

        // When: interpreting stream with syntactic error
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());

        // Then: fails mentioning Syntactic error
        assertFalse(res.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) res)
                        .error()
                        .contains("Syntactic error: Unexpected token"));

        // Already prefixed message
        syntactic.Parser<Node> parser2 =
                stream -> Result.failure("Syntactic error: already prefixed");
        DefaultInterpreter interpreter2 =
                new DefaultInterpreter(
                        parser2,
                        new SemanticChecker(),
                        new DefaultStatementExecutor(System.out::println));
        Result<SemanticEnvironment> res2 =
                interpreter2.interpret(startStream, new SemanticEnvironment(), new Environment());
        assertFalse(res2.isCorrect());
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter stream interpretation | When: semantic checker fails | Then:"
                    + " returns semantic error failure")
    void interpreterStream_whenSemanticErrorOccursInStream_thenReturnsSemanticError() {
        // What: checker that fails with semantic error
        Node node1 =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("a", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        TokenStream endStream = new FakeTokenStream(true);
        TokenStream startStream = new FakeTokenStream(false);

        syntactic.Parser<Node> parser =
                stream -> Result.success(new IterationStep<>(node1, endStream));
        SemanticChecker checker =
                new SemanticChecker() {
                    @Override
                    public Result<SemanticEnvironment> checkNode(
                            Node node, SemanticEnvironment env) {
                        return Result.failure("Type mismatch");
                    }
                };
        StatementExecutor executor = (statement, env) -> Result.success(null);

        DefaultInterpreter interpreter = new DefaultInterpreter(parser, checker, executor);

        // When: interpreting stream with semantic error
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());

        // Then: fails mentioning Semantic error
        assertFalse(res.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) res)
                        .error()
                        .contains("Semantic error: Type mismatch"));
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter stream interpretation | When: statement executor fails at"
                    + " runtime | Then: returns runtime error failure")
    void interpreterStream_whenRuntimeErrorOccursInStream_thenReturnsRuntimeError() {
        // What: executor that fails at runtime
        Node node1 =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("a", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        TokenStream endStream = new FakeTokenStream(true);
        TokenStream startStream = new FakeTokenStream(false);

        syntactic.Parser<Node> parser =
                stream -> Result.success(new IterationStep<>(node1, endStream));
        SemanticChecker checker =
                new SemanticChecker() {
                    @Override
                    public Result<SemanticEnvironment> checkNode(
                            Node node, SemanticEnvironment env) {
                        return Result.success(env);
                    }
                };
        StatementExecutor executor = (statement, env) -> Result.failure("Division by zero");

        DefaultInterpreter interpreter = new DefaultInterpreter(parser, checker, executor);

        // When: interpreting stream with runtime error
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());

        // Then: fails mentioning Runtime error
        assertFalse(res.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) res)
                        .error()
                        .contains("Runtime error: Division by zero"));
    }

    @Test
    @DisplayName(
            "What: DefaultInterpreter ProgramNode interpretation | When: executor fails | Then:"
                    + " returns runtime error failure")
    void interpreterStream_whenRuntimeErrorOccursInProgramNode_thenReturnsRuntimeError() {
        // What: ProgramNode and executor returning failure
        Node node1 =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("a", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        ProgramNode program = new ProgramNode(List.of(node1), 1, 1);
        StatementExecutor executor =
                (statement, env) -> Result.failure("Runtime error: Cannot execute");

        DefaultInterpreter interpreter =
                new DefaultInterpreter(null, new SemanticChecker(), executor);

        // When: interpreting program
        Result<Void> res = interpreter.interpret(program);

        // Then: fails with runtime error
        assertFalse(res.isCorrect());
        assertTrue(((IncorrectResult<Void>) res).error().contains("Runtime error: Cannot execute"));
    }
}
