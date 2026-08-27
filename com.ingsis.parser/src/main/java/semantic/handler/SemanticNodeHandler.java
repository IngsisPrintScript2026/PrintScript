/*
 * My Project
 */

package semantic.handler;

import node.Node;
import result.Result;
import semantic.environment.SemanticEnvironment;

public interface SemanticNodeHandler<T extends Node> {
    Class<T> nodeType();

    Result<SemanticEnvironment> check(T node, SemanticEnvironment env);

    default Result<SemanticEnvironment> checkUntyped(Node node, SemanticEnvironment env) {
        return check(nodeType().cast(node), env);
    }
}
