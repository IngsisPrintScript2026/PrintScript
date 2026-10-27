/*
 * My Project
 */

package interpreter.fake;

import environment.Environment;
import executor.StatementExecutor;
import java.util.ArrayList;
import java.util.List;
import node.Node;
import result.Result;

/** Statement executor that records statements and answers with a fixed result. */
public final class FakeStatementExecutor implements StatementExecutor {
    private final Result<Void> answer;
    private final List<Node> executed = new ArrayList<>();

    private FakeStatementExecutor(Result<Void> answer) {
        this.answer = answer;
    }

    public static FakeStatementExecutor succeeding() {
        return new FakeStatementExecutor(Result.success(null));
    }

    public static FakeStatementExecutor failingWith(String error) {
        return new FakeStatementExecutor(Result.failure(error));
    }

    @Override
    public Result<Void> execute(Node statement, Environment env) {
        executed.add(statement);
        return answer;
    }

    public List<Node> executed() {
        return List.copyOf(executed);
    }
}
