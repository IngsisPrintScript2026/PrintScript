/*
 * My Project
 */

package interpreter.fake;

import environment.Environment;
import executor.StatementExecutor;
import java.util.ArrayList;
import java.util.List;
import node.StatementNode;
import result.Result;

public final class FakeStatementExecutor implements StatementExecutor {
    private final Result<Void> answer;
    private final List<StatementNode> executed = new ArrayList<>();

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
    public Result<Void> execute(StatementNode statement, Environment env) {
        executed.add(statement);
        return answer;
    }

    public List<StatementNode> executed() {
        return List.copyOf(executed);
    }
}
