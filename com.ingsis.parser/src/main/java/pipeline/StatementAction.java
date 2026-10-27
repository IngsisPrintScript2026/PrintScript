/*
 * My Project
 */

package pipeline;

import node.Node;
import result.Result;
import semantic.environment.SemanticEnvironment;

/** What to do with each statement once it was parsed and passed the semantic check. */
@FunctionalInterface
public interface StatementAction {
    StatementAction NONE = (statement, env) -> Result.success(null);

    Result<Void> apply(Node statement, SemanticEnvironment env);
}
