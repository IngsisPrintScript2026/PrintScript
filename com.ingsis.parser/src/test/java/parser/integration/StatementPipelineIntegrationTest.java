/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import node.expression.function.CallFunctionNode;
import node.expression.literal.DataType;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import parser.fake.SpyStatementAction;
import parser.fake.StubFailingStatementAction;
import parser.support.Sources;
import pipeline.StatementAction;
import pipeline.StatementPipeline;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import version.Version;

/** Real CharStream + Lexer + LazyTokenStream + ParserFactory + SemanticChecker on source text. */
class StatementPipelineIntegrationTest {

    private static Result<SemanticEnvironment> run(
            Version version, String source, StatementAction action) {
        return new StatementPipeline(version)
                .run(Sources.tokens(source), new SemanticEnvironment(), action);
    }

    private static String errorOf(Result<SemanticEnvironment> result) {
        assertFalse(result.isCorrect());
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    @Test
    @DisplayName(
            "Given: a three-statement program | When: running the 1.0 pipeline | Then: the action"
                    + " receives declaration, assignment and call in order")
    void pipeline_whenProgramIsValid_thenActionReceivesEveryStatementInOrder() {
        // Given
        String source = "let x: number = 1;\nx = x + 1;\nprintln(x);";
        SpyStatementAction spy = new SpyStatementAction();

        // When
        Result<SemanticEnvironment> result = run(Version.V_1_0, source, spy);

        // Then
        assertTrue(result.isCorrect());
        assertEquals(3, spy.statements().size());
        assertInstanceOf(DeclarationKeywordNode.class, spy.statements().get(0));
        assertInstanceOf(AssignNode.class, spy.statements().get(1));
        assertInstanceOf(CallFunctionNode.class, spy.statements().get(2));
    }

    @Test
    @DisplayName(
            "Given: a declaration | When: running the pipeline | Then: the final environment"
                    + " contains the variable with its declared type")
    void pipeline_whenDeclarationSucceeds_thenFinalEnvironmentHasVariable() {
        // Given
        String source = "let name: string = \"Ada\";";

        // When
        Result<SemanticEnvironment> result = run(Version.V_1_0, source, StatementAction.NONE);

        // Then
        SemanticEnvironment env = ((CorrectResult<SemanticEnvironment>) result).value();
        assertEquals(DataType.STRING, env.lookup("name").orElseThrow().type());
    }

    @Test
    @DisplayName(
            "Given: a declaration followed by a statement | When: the action runs | Then: the"
                    + " second statement sees the variable declared by the first")
    void pipeline_whenSeveralStatements_thenEnvironmentIsThreadedToTheAction() {
        // Given
        String source = "let a: number = 1;\nprintln(a);";
        SpyStatementAction spy = new SpyStatementAction();

        // When
        run(Version.V_1_0, source, spy);

        // Then
        assertTrue(spy.environments().get(1).lookup("a").isPresent());
    }

    @Test
    @DisplayName(
            "Given: 'let x: number;' then 'x = \"hola\";' | When: running | Then: semantic error on"
                    + " the second statement with its full range")
    void pipeline_whenAssigningStringToNumberVariable_thenSemanticErrorWithRange() {
        // Given
        String source = "let x: number;\nx = \"hola\";";

        // When
        String error = errorOf(run(Version.V_1_0, source, StatementAction.NONE));

        // Then
        assertTrue(error.startsWith("Semantic error: "));
        assertTrue(error.contains("'x'"));
        assertTrue(error.contains("[from line 2, column 1 to line 2, column 11]"));
    }

    @Test
    @DisplayName(
            "Given: a statement missing its ';' before another statement | When: running | Then:"
                    + " syntactic error naming the unexpected token and the statement start")
    void pipeline_whenSemicolonMissing_thenSyntacticErrorWithTokenAndStart() {
        // Given
        String source = "let a: number = 1;\nprintln(a)\nprintln(2);";

        // When
        String error = errorOf(run(Version.V_1_0, source, StatementAction.NONE));

        // Then
        assertTrue(error.startsWith("Syntactic error: Unexpected token 'println'"));
        assertTrue(error.contains("from line 3, column 1 to line 3, column 7"));
        assertTrue(error.endsWith("[in statement starting at line 2, column 1]"));
    }

    @Test
    @DisplayName(
            "Given: a source that ends in the middle of a statement | When: running | Then:"
                    + " syntactic error 'Unexpected end of file'")
    void pipeline_whenSourceEndsMidStatement_thenUnexpectedEndOfFile() {
        // Given
        String source = "let a: number = 1";

        // When
        String error = errorOf(run(Version.V_1_0, source, StatementAction.NONE));

        // Then
        assertTrue(error.startsWith("Syntactic error: Unexpected end of file"));
    }

    @Test
    @DisplayName(
            "Given: an unknown char inside a statement | When: running | Then: the error keeps"
                    + " the lexical message with char and position")
    void pipeline_whenUnknownCharInsideStatement_thenReportsLexicalErrorDetails() {
        // Given
        String source = "let x: number = 5 @;";

        // When
        String error = errorOf(run(Version.V_1_0, source, StatementAction.NONE));

        // Then
        assertTrue(error.contains("Lexical error: unrecognized character '@'"));
        assertTrue(error.contains("line 1, column 19"));
    }

    @Test
    @DisplayName(
            "Given: an action that fails on the first statement | When: running real source |"
                    + " Then: runtime error with the range of that statement")
    void pipeline_whenActionFails_thenRuntimeErrorWithStatementRange() {
        // Given
        String source = "println(1 / 0);";
        StatementAction failing = new StubFailingStatementAction("division by zero");

        // When
        String error = errorOf(run(Version.V_1_0, source, failing));

        // Then
        assertEquals(
                "Runtime error: division by zero [from line 1, column 1 to line 1, column 15]",
                error);
    }

    @Test
    @DisplayName(
            "Given: an empty source | When: running | Then: succeeds and the action is never"
                    + " called")
    void pipeline_whenSourceIsEmpty_thenSucceedsWithoutStatements() {
        // Given
        SpyStatementAction spy = new SpyStatementAction();

        // When
        Result<SemanticEnvironment> result = run(Version.V_1_1, "   \n  ", spy);

        // Then
        assertTrue(result.isCorrect());
        assertTrue(spy.statements().isEmpty());
    }
}
