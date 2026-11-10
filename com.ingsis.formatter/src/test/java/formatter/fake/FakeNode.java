/*
 * My Project
 */

package formatter.fake;

import java.util.List;
import node.Node;

public record FakeNode(String symbol) implements Node {
    @Override
    public Integer line() {
        return 1;
    }

    @Override
    public Integer column() {
        return 1;
    }

    @Override
    public List<? extends Node> children() {
        return List.of();
    }
}
