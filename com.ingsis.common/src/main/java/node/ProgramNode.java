/*
 * My Project
 */

package node;

import java.util.List;

public record ProgramNode(List<StatementNode> statements, Integer line, Integer column)
        implements Node {

    public ProgramNode {
        statements = List.copyOf(statements);
    }

    @Override
    public String symbol() {
        return "PROGRAM";
    }

    @Override
    public List<StatementNode> children() {
        return statements;
    }
}
