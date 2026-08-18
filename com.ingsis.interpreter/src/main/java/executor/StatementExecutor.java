package executor;

import environment.Environment;
import node.Node;

public interface StatementExecutor {
    void execute(Node statement, Environment env);
}
