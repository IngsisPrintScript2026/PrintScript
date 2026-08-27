/*
 * My Project
 */

package semantic;

import node.Node;
import semantic.environment.SemanticEnvironment;
import tokenstream.TokenStream;

public record SemanticStep(Node node, SemanticEnvironment updatedEnv, TokenStream nextStream) {}
