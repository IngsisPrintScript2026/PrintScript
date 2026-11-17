/*
 * My Project
 */

package node;

import java.util.List;
import node.visitor.NodeVisitor;
import position.Position;
import position.SourceRange;

// Composite an PODO (Plain Old Data Object)
public interface Node {
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

    default <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visitDefault(this, context);
    }
}
