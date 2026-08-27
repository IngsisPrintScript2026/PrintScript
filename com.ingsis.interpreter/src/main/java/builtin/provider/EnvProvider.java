/*
 * My Project
 */

package builtin.provider;

@FunctionalInterface
public interface EnvProvider {
    String getEnv(String name);
}
