/*
 * My Project
 */

package parser.fake;

import java.util.ArrayList;
import java.util.List;
import node.Node;
import pipeline.StatementAction;
import result.Result;
import semantic.environment.SemanticEnvironment;

/** Records every statement (and the environment it was checked against) in arrival order. */
public final class SpyStatementAction implements StatementAction {
    private final List<Node> statements = new ArrayList<>();
    private final List<SemanticEnvironment> environments = new ArrayList<>();

    @Override
    public Result<Void> apply(Node statement, SemanticEnvironment env) {
        statements.add(statement);
        environments.add(env);
        return Result.success(null);
    }

    public List<Node> statements() {
        return List.copyOf(statements);
    }

    public List<SemanticEnvironment> environments() {
        return List.copyOf(environments);
    }
}
