/*
 * My Project
 */

package node;

import java.util.List;
import position.Position;
import position.SourceRange;

/**
 * Raíz del AST como tipo suma cerrado (al estilo de un {@code enum} de Rust).
 *
 * <p>La jerarquía completa es:
 *
 * <pre>
 * Node           = ProgramNode | StatementNode
 * StatementNode  = DeclarationKeywordNode | AssignNode | IfKeywordNode | WhileKeywordNode
 *                | ExpressionNode
 * ExpressionNode = LiteralNode | IdentifierNode | OperatorNode | CallFunctionNode
 *                | NilExpressionNode
 * LiteralNode    = NumberLiteralNode | StringLiteralNode | BooleanLiteralNode
 * </pre>
 *
 * <p>Como todas las interfaces son {@code sealed}, un {@code switch} sobre cualquiera de ellas sin
 * rama {@code default} es exhaustivo: si se agrega un nodo nuevo, el compilador marca cada {@code
 * switch} que no lo contemple. Por eso los consumidores no deben usar {@code default}.
 */
public sealed interface Node permits ProgramNode, StatementNode {
    Integer line();

    Integer column();

    String symbol();

    List<? extends Node> children();

    /**
     * Rango (inicio y fin) que ocupa el nodo en el código fuente. Por defecto es solo la posición
     * de inicio; los nodos que conocen su extensión lo redefinen.
     */
    default SourceRange range() {
        return SourceRange.at(new Position(line(), column()));
    }
}
