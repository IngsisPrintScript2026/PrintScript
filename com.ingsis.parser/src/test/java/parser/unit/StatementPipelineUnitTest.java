/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static parser.support.Sources.token;

import java.util.List;
import node.Node;
import node.expression.function.CallFunctionNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import parser.fake.FakeCallPerTokensParser;
import parser.fake.SpyStatementAction;
import parser.fake.StubCallSemanticHandler;
import parser.fake.StubFailingParser;
import parser.fake.StubFailingStatementAction;
import pipeline.StatementAction;
import pipeline.StatementPipeline;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.TokenType;
import tokenstream.TokenStream;
import tokenstream.TokenStreamAdapter;

class StatementPipelineUnitTest {

    /** Two statements on line 1: {@code ok ;} (cols 1-3) and {@code bad ;} (cols 5-8). */
    private static TokenStream okThenBad() {
        return new TokenStreamAdapter(
                List.of(
                        token(TokenType.IDENTIFIER, "ok", 1),
                        token(TokenType.SEMICOLON, ";", 3),
                        token(TokenType.IDENTIFIER, "bad", 5),
                        token(TokenType.SEMICOLON, ";", 8)),
                0);
    }

    private static SemanticChecker checkerRejecting(String name, String message) {
        return new SemanticChecker(List.of(new StubCallSemanticHandler(name, message)));
    }

    private static StatementPipeline pipelineRejecting(String name, String message) {
        return new StatementPipeline(
                new FakeCallPerTokensParser(2), checkerRejecting(name, message));
    }

    private static String errorOf(Result<SemanticEnvironment> result) {
        assertFalse(result.isCorrect());
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    private static String nameOf(Node statement) {
        return ((CallFunctionNode) statement).identifierNode().name();
    }

    @Test
    @DisplayName(
            "Given: two valid statements | When: running the pipeline with a spy action | Then:"
                    + " the action receives both statements in order")
    void pipeline_whenAllStatementsValid_thenActionReceivesEachInOrder() {
        // Given
        StatementPipeline pipeline = pipelineRejecting("none", "unused");
        SpyStatementAction spy = new SpyStatementAction();

        // When
        Result<SemanticEnvironment> result =
                pipeline.run(okThenBad(), new SemanticEnvironment(), spy);

        // Then
        assertTrue(result.isCorrect());
        assertEquals(List.of("ok", "bad"), spy.statements().stream().map(n -> nameOf(n)).toList());
    }

    @Test
    @DisplayName(
            "Given: a checker that rejects 'bad' | When: running | Then: fails with a semantic"
                    + " error carrying the range of the whole statement")
    void pipeline_whenCheckerRejects_thenSemanticErrorWithStatementRange() {
        // Given
        StatementPipeline pipeline = pipelineRejecting("bad", "boom");

        // When
        Result<SemanticEnvironment> result =
                pipeline.run(okThenBad(), new SemanticEnvironment(), StatementAction.NONE);

        // Then
        assertEquals(
                "Semantic error: boom [from line 1, column 5 to line 1, column 8]",
                errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a checker that rejects the second statement | When: running | Then: the"
                    + " action only saw the first statement")
    void pipeline_whenCheckerRejects_thenStopsBeforeApplyingRejectedStatement() {
        // Given
        StatementPipeline pipeline = pipelineRejecting("bad", "boom");
        SpyStatementAction spy = new SpyStatementAction();

        // When
        pipeline.run(okThenBad(), new SemanticEnvironment(), spy);

        // Then
        assertEquals(1, spy.statements().size());
        assertEquals("ok", nameOf(spy.statements().get(0)));
    }

    @Test
    @DisplayName(
            "Given: a checker error already labelled 'Semantic error' | When: running | Then: the"
                    + " label is not duplicated")
    void pipeline_whenSemanticErrorAlreadyLabelled_thenNoDoublePrefix() {
        // Given
        StatementPipeline pipeline = pipelineRejecting("bad", "Semantic error: boom");

        // When
        String error =
                errorOf(pipeline.run(okThenBad(), new SemanticEnvironment(), StatementAction.NONE));

        // Then
        assertTrue(error.startsWith("Semantic error: boom ["));
        assertFalse(error.contains("Semantic error: Semantic error"));
    }

    @Test
    @DisplayName(
            "Given: no semantic checker (null) | When: running a statement the stub would reject |"
                    + " Then: the check is skipped and the action still runs")
    void pipeline_whenCheckerIsNull_thenSkipsSemanticCheck() {
        // Given
        StatementPipeline pipeline = new StatementPipeline(new FakeCallPerTokensParser(2), null);
        SpyStatementAction spy = new SpyStatementAction();

        // When
        Result<SemanticEnvironment> result =
                pipeline.run(okThenBad(), new SemanticEnvironment(), spy);

        // Then
        assertTrue(result.isCorrect());
        assertEquals(2, spy.statements().size());
    }

    @Test
    @DisplayName(
            "Given: an action that fails | When: running | Then: fails with a runtime error"
                    + " carrying the statement range")
    void pipeline_whenActionFails_thenRuntimeErrorWithRange() {
        // Given
        StatementPipeline pipeline = pipelineRejecting("none", "unused");

        // When
        Result<SemanticEnvironment> result =
                pipeline.run(
                        okThenBad(),
                        new SemanticEnvironment(),
                        new StubFailingStatementAction("division by zero"));

        // Then
        assertEquals(
                "Runtime error: division by zero [from line 1, column 1 to line 1, column 3]",
                errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: an action error already labelled 'Runtime error' | When: running | Then: the"
                    + " label is not duplicated")
    void pipeline_whenRuntimeErrorAlreadyLabelled_thenNoDoublePrefix() {
        // Given
        StatementPipeline pipeline = pipelineRejecting("none", "unused");

        // When
        String error =
                errorOf(
                        pipeline.run(
                                okThenBad(),
                                new SemanticEnvironment(),
                                new StubFailingStatementAction("Runtime error: oops")));

        // Then
        assertTrue(error.startsWith("Runtime error: oops ["));
        assertFalse(error.contains("Runtime error: Runtime error"));
    }

    @Test
    @DisplayName(
            "Given: a parser that fails | When: running | Then: fails with a syntactic error"
                    + " pointing at the start of the statement")
    void pipeline_whenParserFails_thenSyntacticErrorWithStatementStart() {
        // Given
        StatementPipeline pipeline =
                new StatementPipeline(new StubFailingParser<>("Unexpected token ';'"), null);

        // When
        Result<SemanticEnvironment> result =
                pipeline.run(okThenBad(), new SemanticEnvironment(), StatementAction.NONE);

        // Then
        assertEquals(
                "Syntactic error: Unexpected token ';' [in statement starting at line 1, column"
                        + " 1]",
                errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a parser error already labelled 'Syntactic error' | When: running | Then: the"
                    + " label is not duplicated")
    void pipeline_whenSyntacticErrorAlreadyLabelled_thenNoDoublePrefix() {
        // Given
        StatementPipeline pipeline =
                new StatementPipeline(new StubFailingParser<>("Syntactic error: oops"), null);

        // When
        String error =
                errorOf(pipeline.run(okThenBad(), new SemanticEnvironment(), StatementAction.NONE));

        // Then
        assertTrue(error.startsWith("Syntactic error: oops ["));
        assertFalse(error.contains("Syntactic error: Syntactic error"));
    }

    @Test
    @DisplayName(
            "Given: a parser that reports EOF | When: running | Then: ends successfully with the"
                    + " initial environment")
    void pipeline_whenParserReportsEof_thenEndsSuccessfully() {
        // Given
        StatementPipeline pipeline =
                new StatementPipeline(new StubFailingParser<>("EOF: no more tokens"), null);
        SemanticEnvironment env = new SemanticEnvironment();

        // When
        Result<SemanticEnvironment> result = pipeline.run(okThenBad(), env, StatementAction.NONE);

        // Then
        assertSame(env, ((CorrectResult<SemanticEnvironment>) result).value());
    }

    @Test
    @DisplayName(
            "Given: an empty token stream | When: running | Then: succeeds without calling the"
                    + " action")
    void pipeline_whenStreamIsEmpty_thenSucceedsWithoutStatements() {
        // Given
        StatementPipeline pipeline = pipelineRejecting("none", "unused");
        SpyStatementAction spy = new SpyStatementAction();

        // When
        Result<SemanticEnvironment> result =
                pipeline.run(new TokenStreamAdapter(List.of(), 0), new SemanticEnvironment(), spy);

        // Then
        assertTrue(result.isCorrect());
        assertTrue(spy.statements().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: the NONE action | When: applying it to any statement | Then: it succeeds"
                    + " doing nothing")
    void statementActionNone_whenApplied_thenSucceeds() {
        // Given
        StatementAction action = StatementAction.NONE;

        // When
        Result<Void> result = action.apply(null, new SemanticEnvironment());

        // Then
        assertTrue(result.isCorrect());
    }
}
