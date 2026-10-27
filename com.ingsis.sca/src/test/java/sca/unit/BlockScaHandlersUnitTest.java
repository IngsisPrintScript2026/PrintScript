/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sca.fake.ScaNodes.let;

import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.literal.BooleanLiteralNode;
import node.keyword.IfKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sca.ASTSca;
import sca.ScaContext;
import sca.handler.IfScaHandler;
import sca.handler.ProgramScaHandler;
import semantic.environment.SemanticEnvironment;

class BlockScaHandlersUnitTest {
    private static final ScaContext SNAKE = new ScaContext("snake case", false, false);

    private static IfKeywordNode ifNode(List<Node> thenBody, List<Node> elseBody) {
        return new IfKeywordNode(new BooleanLiteralNode(true, 1, 4), thenBody, elseBody, 1, 1);
    }

    private static List<String> checkIf(IfKeywordNode node) {
        return new IfScaHandler().check(node, new SemanticEnvironment(), SNAKE, new ASTSca(SNAKE));
    }

    @Test
    @DisplayName(
            "Given: an if whose then-body declares 'badName' | When: checking the if | Then:"
                    + " reports the violation of the inner statement")
    void ifHandler_whenThenBodyViolates_thenReportsIt() {
        // Given
        IfKeywordNode node = ifNode(List.of(let("badName", 2, 9)), List.of());

        // When
        List<String> violations = checkIf(node);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'badName'"));
        assertTrue(violations.get(0).contains("line 2"));
    }

    @Test
    @DisplayName(
            "Given: an if whose else-body declares 'otherName' | When: checking the if | Then:"
                    + " reports the violation of the else statement")
    void ifHandler_whenElseBodyViolates_thenReportsIt() {
        // Given
        IfKeywordNode node = ifNode(List.of(), List.of(let("otherName", 4, 9)));

        // When
        List<String> violations = checkIf(node);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'otherName'"));
    }

    @Test
    @DisplayName(
            "Given: violations in both branches | When: checking the if | Then: reports then-body"
                    + " violations before else-body ones")
    void ifHandler_whenBothBranchesViolate_thenReportsBothInOrder() {
        // Given
        IfKeywordNode node = ifNode(List.of(let("thenName", 2, 9)), List.of(let("elseName", 4, 9)));

        // When
        List<String> violations = checkIf(node);

        // Then
        assertEquals(2, violations.size());
        assertTrue(violations.get(0).contains("'thenName'"));
        assertTrue(violations.get(1).contains("'elseName'"));
    }

    @Test
    @DisplayName(
            "Given: an if whose branches respect the convention | When: checking the if | Then:"
                    + " reports no violation")
    void ifHandler_whenBranchesAreClean_thenNoViolation() {
        // Given
        IfKeywordNode node = ifNode(List.of(let("good_name", 2, 9)), List.of());

        // When
        List<String> violations = checkIf(node);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a program with one clean and two violating declarations | When: checking the"
                    + " program | Then: reports one violation per offending statement")
    void programHandler_whenStatementsViolate_thenCollectsAll() {
        // Given
        ProgramNode program =
                new ProgramNode(
                        List.of(
                                let("first_ok", 1, 5),
                                let("secondBad", 2, 5),
                                let("thirdBad", 3, 5)),
                        1,
                        1);

        // When
        List<String> violations =
                new ProgramScaHandler()
                        .check(program, new SemanticEnvironment(), SNAKE, new ASTSca(SNAKE));

        // Then
        assertEquals(2, violations.size());
        assertTrue(violations.get(0).contains("'secondBad'"));
        assertTrue(violations.get(1).contains("'thirdBad'"));
    }

    @Test
    @DisplayName(
            "Given: an empty program | When: checking the program | Then: reports no violation")
    void programHandler_whenProgramIsEmpty_thenNoViolation() {
        // Given
        ProgramNode program = new ProgramNode(List.of(), 1, 1);

        // When
        List<String> violations =
                new ProgramScaHandler()
                        .check(program, new SemanticEnvironment(), SNAKE, new ASTSca(SNAKE));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: the block handlers | When: asking their node types | Then: they handle if and"
                    + " program nodes")
    void blockHandlers_whenAskingNodeTypes_thenAreIfAndProgram() {
        // Given
        IfScaHandler ifHandler = new IfScaHandler();
        ProgramScaHandler programHandler = new ProgramScaHandler();

        // When
        Class<?> ifType = ifHandler.nodeType();
        Class<?> programType = programHandler.nodeType();

        // Then
        assertEquals(IfKeywordNode.class, ifType);
        assertEquals(ProgramNode.class, programType);
    }
}
