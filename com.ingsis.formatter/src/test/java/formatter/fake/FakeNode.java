/*
 * My Project
 */

package formatter.fake;

import java.util.List;
import node.Node;

/** Statement node without a dedicated visitor method, so visitors fall back to visitDefault. */
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
