/*
 * My Project
 */

package pipeline;

import node.StatementNode;
import result.Result;
import semantic.environment.SemanticEnvironment;

@FunctionalInterface
public interface StatementAction {
    StatementAction NONE = (statement, env) -> Result.success(null);

    Result<Void> apply(StatementNode statement, SemanticEnvironment env);
}
