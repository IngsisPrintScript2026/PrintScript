/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import node.BooleanLiteralNode;
import node.CallFunctionNode;
import node.IdentifierNode;
import node.NumberLiteralNode;
import node.OperatorNode;
import node.StringLiteralNode;
import node.expression.operator.OperatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import position.SourceRange;

class NodeRangeUnitTest {

    private static SourceRange range(int startLine, int startCol, int endLine, int endCol) {
        return new SourceRange(new Position(startLine, startCol), new Position(endLine, endCol));
    }

    @Test
    @DisplayName(
            "Given: text 'abc' starting at 2:5 | When: computing its span | Then: ends on the"
                    + " last character, 2:7")
    void spanning_whenSingleLineText_thenEndsOnLastCharacter() {
        // Given
        Position start = new Position(2, 5);

        // When
        SourceRange span = SourceRange.spanning(start, "abc");

        // Then
        assertEquals(range(2, 5, 2, 7), span);
    }

    @Test
    @DisplayName(
            "Given: text with a line break starting at 1:10 | When: computing its span | Then:"
                    + " ends on the next line")
    void spanning_whenTextHasNewline_thenEndsOnFollowingLine() {
        // Given
        Position start = new Position(1, 10);

        // When
        SourceRange span = SourceRange.spanning(start, "\"ab\ncd\"");

        // Then
        assertEquals(range(1, 10, 2, 3), span);
    }

    @Test
    @DisplayName(
            "Given: empty text or an unknown start | When: computing its span | Then: falls back"
                    + " to the start or to an unknown range")
    void spanning_whenEmptyOrUnknown_thenFallsBack() {
        // Given
        Position start = new Position(3, 4);

        // When
        SourceRange empty = SourceRange.spanning(start, "");
        SourceRange unknown = SourceRange.spanning(null, "abc");
        SourceRange partial = SourceRange.spanning(new Position(null, 1), "abc");

        // Then
        assertEquals(SourceRange.at(start), empty);
        assertEquals("at an unknown position", unknown.toString());
        assertEquals("at an unknown position", partial.toString());
    }

    @Test
    @DisplayName(
            "Given: identifier 'my_name' at 2:5 | When: asking its range | Then: covers the"
                    + " whole name, 2:5 to 2:11")
    void identifier_whenAskedForRange_thenCoversTheName() {
        // Given
        IdentifierNode id = new IdentifierNode("my_name", 2, 5);

        // When
        SourceRange span = id.range();

        // Then
        assertEquals("from line 2, column 5 to line 2, column 11", span.toString());
    }

    @Test
    @DisplayName(
            "Given: literals of each type | When: asking their range | Then: strings include"
                    + " the quotes and numbers keep their written form")
    void literals_whenAskedForRange_thenCoverTheirSourceText() {
        // Given
        StringLiteralNode text = new StringLiteralNode("Joe", 1, 10);
        NumberLiteralNode number = new NumberLiteralNode(new BigDecimal("3.50"), 1, 3);
        BooleanLiteralNode bool = new BooleanLiteralNode(false, 4, 1);

        // When / Then
        assertEquals(range(1, 10, 1, 14), text.range());
        assertEquals(range(1, 3, 1, 6), number.range());
        assertEquals(range(4, 1, 4, 5), bool.range());
    }

    @Test
    @DisplayName(
            "Given: a + 10 with 'a' at 1:9 | When: asking the operator range | Then: goes from"
                    + " the left operand start to the right operand end")
    void operator_whenAskedForRange_thenSpansBothOperands() {
        // Given
        OperatorNode sum =
                new OperatorNode(
                        OperatorType.PLUS,
                        new IdentifierNode("a", 1, 9),
                        new NumberLiteralNode(new BigDecimal("10"), 1, 13),
                        1,
                        11);

        // When
        SourceRange span = sum.range();

        // Then
        assertEquals(range(1, 9, 1, 14), span);
    }

    @Test
    @DisplayName(
            "Given: an operator without operands | When: asking its range | Then: falls back to"
                    + " the operator position")
    void operator_whenOperandsMissing_thenUsesItsOwnPosition() {
        // Given
        OperatorNode sum = new OperatorNode(OperatorType.PLUS, null, null, 2, 7);

        // When
        SourceRange span = sum.range();

        // Then
        assertEquals(range(2, 7, 2, 7), span);
    }

    @Test
    @DisplayName(
            "Given: a node that does not know its end | When: asking its range | Then: the"
                    + " default range is just its start")
    void node_whenNoSpecificRange_thenDefaultsToStart() {
        // Given
        CallFunctionNode call =
                new CallFunctionNode(new IdentifierNode("println", 3, 1), List.of(), 3, 1);

        // When
        SourceRange span = call.range();

        // Then
        assertEquals(range(3, 1, 3, 1), span);
    }
}
