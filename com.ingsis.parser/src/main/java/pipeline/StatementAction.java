/*
 * My Project
 */

package pipeline;

import node.Node;
import result.Result;
import semantic.environment.SemanticEnvironment;

@FunctionalInterface
public interface StatementAction {
    StatementAction NONE = (statement, env) -> Result.success(null);

    Result<Void> apply(Node statement, SemanticEnvironment env);
}
