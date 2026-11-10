/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import node.Node;
import node.expression.Identifier.IdentifierNode;
import node.keyword.WhileKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import parser.support.Sources;
import pipeline.StatementAction;
import pipeline.StatementPipeline;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import syntactic.parser.ParserFactory;
import version.Version;

class PrintScript12RulesIntegrationTest {

    private static final String LOOP =
            "let go: boolean = true;\nwhile (go) {\n  println(\"x\");\n  go = false;\n}";

    private static Result<SemanticEnvironment> check(Version version, String source) {
        return new StatementPipeline(version)
                .run(Sources.tokens(source), new SemanticEnvironment(), StatementAction.NONE);
    }

    private static String errorOf(Version version, String source) {
        Result<SemanticEnvironment> result = check(version, source);
        assertFalse(result.isCorrect());
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    @Test
    @DisplayName(
            "Given: 'while (go) { ... }' | When: parsing in 1.2 | Then: a WhileKeywordNode with"
                    + " its condition, body and position")
    void v12_whenWhileParsed_thenWhileNodeWithConditionAndBody() {
        // Given
        String source = "while (go) {\n  println(\"x\");\n  go = false;\n}";

        // When
        Result<IterationStep<Node>> parsed =
                ParserFactory.createParser(Version.V_1_2).parse(Sources.tokens(source));

        // Then
        assertTrue(parsed.isCorrect());
        Node statement = ((CorrectResult<IterationStep<Node>>) parsed).value().value();
        WhileKeywordNode whileNode = assertInstanceOf(WhileKeywordNode.class, statement);
        assertEquals("go", assertInstanceOf(IdentifierNode.class, whileNode.condition()).name());
        assertEquals(2, whileNode.body().size());
        assertEquals(1, whileNode.line());
        assertEquals(1, whileNode.column());
    }

    @Test
    @DisplayName(
            "Given: a while over a boolean variable | When: checking in 1.2 | Then: syntax and"
                    + " semantics are valid")
    void v12_whenWhileOverBoolean_thenValid() {
        assertTrue(check(Version.V_1_2, LOOP).isCorrect());
    }

    @Test
    @DisplayName(
            "Given: the same while | When: checking in 1.1 | Then: syntactic error, while is not"
                    + " part of that version")
    void v11_whenWhileUsed_thenSyntacticError() {
        // When
        String error = errorOf(Version.V_1_1, LOOP);

        // Then
        assertTrue(error.contains("Syntactic error"), error);
        assertTrue(error.contains("not valid in PrintScript 1.1"), error);
    }

    @Test
    @DisplayName(
            "Given: a while whose condition is a number | When: checking | Then: semantic error,"
                    + " the condition must be boolean")
    void v12_whenConditionNotBoolean_thenSemanticError() {
        // When
        String error = errorOf(Version.V_1_2, "let n: number = 1;\nwhile (n) {\n}");

        // Then
        assertTrue(error.contains("Condition of 'while' at line 2 must be boolean"), error);
    }

    @Test
    @DisplayName(
            "Given: a while whose condition uses an undeclared variable | When: checking | Then:"
                    + " semantic error naming the variable")
    void v12_whenConditionUndeclared_thenSemanticError() {
        // When
        String error = errorOf(Version.V_1_2, "while (missing) {\n}");

        // Then
        assertTrue(error.contains("Variable 'missing' is not declared"), error);
    }

    @Test
    @DisplayName(
            "Given: a variable declared inside the while body | When: used after the loop | Then:"
                    + " semantic error, the body has its own scope")
    void v12_whenBodyVariableUsedOutside_thenSemanticError() {
        // Given
        String source =
                "let go: boolean = false;\nwhile (go) {\n  let inner: number = 1;\n}\ninner = 2;";

        // When
        String error = errorOf(Version.V_1_2, source);

        // Then
        assertTrue(error.contains("Semantic error"), error);
        assertTrue(error.contains("inner"), error);
    }

    @Test
    @DisplayName(
            "Given: a type error inside the while body | When: checking | Then: the semantic"
                    + " error of the body is reported")
    void v12_whenBodyHasSemanticError_thenReported() {
        // When
        String error =
                errorOf(Version.V_1_2, "let go: boolean = true;\nwhile (go) {\n  go = 5;\n}");

        // Then
        assertTrue(error.contains("Semantic error"), error);
    }

    @Test
    @DisplayName(
            "Given: malformed while headers and bodies | When: checking | Then: each one is a"
                    + " syntactic error")
    void v12_whenWhileMalformed_thenSyntacticError() {
        String declaration = "let go: boolean = true;\n";

        assertTrue(errorOf(Version.V_1_2, declaration + "while go) {\n}").contains("Syntactic"));
        assertTrue(errorOf(Version.V_1_2, declaration + "while () {\n}").contains("Syntactic"));
        assertTrue(errorOf(Version.V_1_2, declaration + "while (go {\n}").contains("Syntactic"));
        assertTrue(errorOf(Version.V_1_2, declaration + "while (go) go;").contains("Syntactic"));
    }
}
