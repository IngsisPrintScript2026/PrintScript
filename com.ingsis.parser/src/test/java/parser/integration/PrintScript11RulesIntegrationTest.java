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
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
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

/** Language rules added in PrintScript 1.1, checked through the real lexer, parser and checker. */
class PrintScript11RulesIntegrationTest {

    private static Result<SemanticEnvironment> check(String source) {
        return new StatementPipeline(Version.V_1_1)
                .run(Sources.tokens(source), new SemanticEnvironment(), StatementAction.NONE);
    }

    private static String errorOf(String source) {
        Result<SemanticEnvironment> result = check(source);
        assertFalse(result.isCorrect());
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    private static Node firstStatement(String source) {
        Result<IterationStep<Node>> parsed =
                ParserFactory.createParser(Version.V_1_1).parse(Sources.tokens(source));
        assertTrue(parsed.isCorrect());
        return ((CorrectResult<IterationStep<Node>>) parsed).value().value();
    }

    @Test
    @DisplayName(
            "Given: 'const pi: number = 3.14;' | When: parsing | Then: an immutable declaration"
                    + " typed NUMBER")
    void v11_whenConstDeclared_thenImmutableDeclaration() {
        // Given
        String source = "const pi: number = 3.14;";

        // When
        DeclarationKeywordNode decl =
                assertInstanceOf(DeclarationKeywordNode.class, firstStatement(source));

        // Then
        assertFalse(decl.isMutable());
        assertEquals(DataType.NUMBER, decl.dataType());
    }

    @Test
    @DisplayName(
            "Given: a const reassigned later | When: checking | Then: semantic error, constants"
                    + " cannot be reassigned")
    void v11_whenConstReassigned_thenSemanticError() {
        // Given
        String source = "const c: number = 1;\nc = 2;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.startsWith("Semantic error"));
        assertTrue(error.contains("Cannot reassign constant variable 'c'"));
        assertTrue(error.contains("from line 2, column 1 to line 2, column 6"));
    }

    @Test
    @DisplayName(
            "Given: a boolean declaration with a literal | When: checking | Then: it is valid and"
                    + " typed BOOLEAN")
    void v11_whenBooleanDeclared_thenValid() {
        // Given
        String source = "let ok: boolean = false;";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        SemanticEnvironment env = ((CorrectResult<SemanticEnvironment>) result).value();
        assertEquals(DataType.BOOLEAN, env.lookup("ok").orElseThrow().type());
    }

    @Test
    @DisplayName(
            "Given: a boolean variable assigned a number | When: checking | Then: semantic type"
                    + " mismatch")
    void v11_whenBooleanGetsNumber_thenTypeMismatch() {
        // Given
        String source = "let ok: boolean = 1;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Cannot assign type NUMBER to variable 'ok' of type BOOLEAN"));
    }

    @Test
    @DisplayName(
            "Given: 'if (true) {..} else {..}' | When: parsing | Then: an if node with one"
                    + " statement in each branch")
    void v11_whenIfElse_thenIfNodeWithBothBranches() {
        // Given
        String source = "if (true) { println(1); } else { println(2); }";

        // When
        IfKeywordNode ifNode = assertInstanceOf(IfKeywordNode.class, firstStatement(source));

        // Then
        assertInstanceOf(BooleanLiteralNode.class, ifNode.condition());
        assertEquals(1, ifNode.thenBody().size());
        assertEquals(1, ifNode.elseBody().size());
    }

    @Test
    @DisplayName("Given: an if without else | When: parsing | Then: the else branch is empty")
    void v11_whenIfWithoutElse_thenElseBranchEmpty() {
        // Given
        String source = "if (true) { println(1); }";

        // When
        IfKeywordNode ifNode = assertInstanceOf(IfKeywordNode.class, firstStatement(source));

        // Then
        assertTrue(ifNode.elseBody().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: an if with a boolean variable as condition | When: checking | Then: it is"
                    + " valid")
    void v11_whenIfConditionIsBooleanVariable_thenValid() {
        // Given
        String source = "let b: boolean = true;\nif (b) { println(\"yes\"); }";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: an if whose condition is a number | When: checking | Then: semantic error,"
                    + " condition must be boolean")
    void v11_whenIfConditionIsNumber_thenMustBeBoolean() {
        // Given
        String source = "if (1) { println(1); }";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("must be boolean"));
    }

    @Test
    @DisplayName(
            "Given: an if whose condition is a string variable | When: checking | Then: semantic"
                    + " error, condition must be boolean")
    void v11_whenIfConditionIsString_thenMustBeBoolean() {
        // Given
        String source = "let s: string = \"x\";\nif (s) { println(1); }";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("must be boolean"));
    }

    @Test
    @DisplayName(
            "Given: 'else if' | When: checking | Then: syntactic error at the second 'if' (else-if"
                    + " is not part of the language)")
    void v11_whenElseIf_thenSyntacticError() {
        // Given
        String source = "if (true) { println(1); } else if (false) { println(2); }";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.startsWith("Syntactic error"));
        assertTrue(error.contains("Unexpected token 'if'"));
    }

    @Test
    @DisplayName(
            "Given: an if block that is never closed | When: checking | Then: syntactic error"
                    + " mentioning the missing '}'")
    void v11_whenBlockNotClosed_thenSyntacticError() {
        // Given
        String source = "if (true) { println(1);";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.startsWith("Syntactic error"));
        assertTrue(error.contains("}"));
    }

    @Test
    @DisplayName(
            "Given: a variable declared inside an if block | When: using it after the block |"
                    + " Then: semantic error, it is not declared outside")
    void v11_whenVariableUsedOutsideItsBlock_thenNotDeclared() {
        // Given
        String source = "if (true) { let a: number = 1; }\nprintln(a);";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Variable 'a' is not declared"));
    }

    @Test
    @DisplayName(
            "Given: readInput assigned to a number | When: checking | Then: valid, readInput"
                    + " adapts to the target type")
    void v11_whenReadInputIntoNumber_thenValid() {
        // Given
        String source = "let n: number = readInput(\"Number?\");";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: readEnv assigned to a boolean | When: checking | Then: valid, readEnv adapts"
                    + " to the target type")
    void v11_whenReadEnvIntoBoolean_thenValid() {
        // Given
        String source = "const debug: boolean = readEnv(\"DEBUG\");";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: a call to an unknown function foo | When: checking | Then: semantic error,"
                    + " foo is not supported in PrintScript 1.1")
    void v11_whenUnknownFunctionCalled_thenNotSupported() {
        // Given
        String source = "foo(1);";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Function 'foo' is not supported in PrintScript 1.1"));
    }

    @Test
    @DisplayName(
            "Given: '!' (not an operator of the language) | When: checking | Then: the error"
                    + " reports a lexical error for '!'")
    void v11_whenUnsupportedOperatorChar_thenLexicalError() {
        // Given
        String source = "let b: boolean = !true;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Lexical error: unrecognized character '!'"));
        assertTrue(error.contains("line 1, column 18"));
    }
}
