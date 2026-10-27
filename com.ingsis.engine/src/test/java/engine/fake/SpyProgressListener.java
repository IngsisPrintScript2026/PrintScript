/*
 * My Project
 */

package engine.fake;

import java.util.ArrayList;
import java.util.List;
import progress.ProgressListener;

/** Records every progress notification it receives. */
public final class SpyProgressListener implements ProgressListener {
    private final List<Long> reported = new ArrayList<>();
    private int clears;
    private int finishes;

    @Override
    public void onCharactersRead(long charactersRead) {
        reported.add(charactersRead);
    }

    @Override
    public void clear() {
        clears++;
    }

    @Override
    public void finish() {
        finishes++;
    }

    public List<Long> reported() {
        return List.copyOf(reported);
    }

    public long lastReported() {
        return reported.isEmpty() ? 0 : reported.get(reported.size() - 1);
    }

    public int clears() {
        return clears;
    }

    public int finishes() {
        return finishes;
    }
}
