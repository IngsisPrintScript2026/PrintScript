package node;


import java.util.List;

public record ProgramNode(List<Node> statements, Integer line, Integer column) implements Node {

    public ProgramNode {
        statements = List.copyOf(statements);
    }

    @Override
    public String symbol() {
        return "PROGRAM";
    }

    @Override
    public List<Node> children() {
        return statements;
    }

}
