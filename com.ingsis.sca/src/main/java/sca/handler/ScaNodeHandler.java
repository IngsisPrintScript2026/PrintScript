/*
 * My Project
 */

package sca.handler;

import java.util.List;
import node.Node;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

public interface ScaNodeHandler<T extends Node> {
    Class<T> nodeType();

    List<String> check(T node, SemanticEnvironment env, ScaContext context, ASTSca sca);

    @SuppressWarnings("unchecked")
    default List<String> checkUntyped(
            Node node, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        return check((T) node, env, context, sca);
    }
}
