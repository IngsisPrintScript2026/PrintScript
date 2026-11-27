/*
 * My Project
 */

package semantic;

import node.StatementNode;
import semantic.environment.SemanticEnvironment;
import tokenstream.TokenStream;

public record SemanticStep(
        StatementNode node, SemanticEnvironment updatedEnv, TokenStream nextStream) {}
