/*
 * My Project
 */

package interpreter;

import static org.junit.jupiter.api.Assertions.*;

import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import executor.DefaultStatementExecutor;
import executor.StatementExecutor;
import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.DataType;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.Test;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

class InterpreterStreamTest {

    private static class DummyTokenStream implements TokenStream {
        private final boolean empty;

        public DummyTokenStream(boolean empty) {
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
    void testAllConstructors() {
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
                        null, new SemanticChecker(), System.out::println, prompt -> "", key -> ""));
    }

    @Test
    void testNullStatementParserFailure() {
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        null,
                        new SemanticChecker(),
                        new DefaultStatementExecutor(System.out::println));
        Result<SemanticEnvironment> result =
                interpreter.interpret(
                        new DummyTokenStream(false), new SemanticEnvironment(), new Environment());
        assertFalse(result.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) result)
                        .error()
                        .contains("Syntactic parser dependency must be injected"));
    }

    @Test
    void testSuccessfulStreamInterpretation() {
        Node node1 =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("a", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        TokenStream endStream = new DummyTokenStream(true);
        TokenStream startStream = new DummyTokenStream(false);

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
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());
        assertTrue(res.isCorrect());
    }

    @Test
    void testParseFailureEOF() {
        TokenStream startStream = new DummyTokenStream(false);
        syntactic.Parser<Node> parser = stream -> Result.failure("EOF");
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        parser,
                        new SemanticChecker(),
                        new DefaultStatementExecutor(System.out::println));

        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());
        assertTrue(res.isCorrect());
    }

    @Test
    void testParseFailureSyntacticError() {
        TokenStream startStream = new DummyTokenStream(false);
        syntactic.Parser<Node> parser = stream -> Result.failure("Unexpected token");
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        parser,
                        new SemanticChecker(),
                        new DefaultStatementExecutor(System.out::println));

        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());
        assertFalse(res.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) res)
                        .error()
                        .contains("Syntactic error: Unexpected token"));

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
    void testSemanticErrorInStream() {
        Node node1 =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("a", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        TokenStream endStream = new DummyTokenStream(true);
        TokenStream startStream = new DummyTokenStream(false);

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
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());
        assertFalse(res.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) res)
                        .error()
                        .contains("Semantic error: Type mismatch"));
    }

    @Test
    void testRuntimeErrorInStream() {
        Node node1 =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("a", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        TokenStream endStream = new DummyTokenStream(true);
        TokenStream startStream = new DummyTokenStream(false);

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
        Result<SemanticEnvironment> res =
                interpreter.interpret(startStream, new SemanticEnvironment(), new Environment());
        assertFalse(res.isCorrect());
        assertTrue(
                ((IncorrectResult<SemanticEnvironment>) res)
                        .error()
                        .contains("Runtime error: Division by zero"));
    }

    @Test
    void testRuntimeErrorInProgramNode() {
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
        Result<Void> res = interpreter.interpret(program);
        assertFalse(res.isCorrect());
        assertTrue(((IncorrectResult<Void>) res).error().contains("Runtime error: Cannot execute"));
    }
}
