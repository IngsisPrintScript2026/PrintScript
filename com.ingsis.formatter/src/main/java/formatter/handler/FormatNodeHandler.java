package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import node.Node;

public interface FormatNodeHandler<T extends Node> {
    Class<T> nodeType();
    String format(T node, FormatContext context, ASTFormatter formatter);

    @SuppressWarnings("unchecked")
    default String formatUntyped(Node node, FormatContext context, ASTFormatter formatter) {
        return format((T) node, context, formatter);
    }
}
