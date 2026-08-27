/*
 * My Project
 */

package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import java.util.stream.Collectors;
import node.ProgramNode;

public class ProgramNodeFormatHandler implements FormatNodeHandler<ProgramNode> {
    @Override
    public Class<ProgramNode> nodeType() {
        return ProgramNode.class;
    }

    @Override
    public String format(ProgramNode program, FormatContext context, ASTFormatter formatter) {
        return program.statements().stream()
                .map(stmt -> formatter.formatStatement(stmt, context))
                .collect(Collectors.joining("\n"));
    }
}
