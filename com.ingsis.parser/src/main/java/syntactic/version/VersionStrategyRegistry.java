package syntactic.version;

import version.Version;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionStrategyRegistry {
    private final Map<Version, VersionStrategy> strategies = new HashMap<>();

    public VersionStrategyRegistry(List<VersionStrategy> strategyList) {
        for (VersionStrategy s : strategyList) {
            strategies.put(s.version(), s);
        }
    }

    public VersionStrategyRegistry() {
        this(List.of(new Version10Strategy(), new Version11Strategy()));
    }

    public VersionStrategy getStrategy(Version version) {
        VersionStrategy strategy = strategies.get(version);
        if (strategy == null) {
            throw new IllegalArgumentException("No VersionStrategy registered for version: " + version);
        }
        return strategy;
    }
}
