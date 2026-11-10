/*
 * My Project
 */

package progress;

public interface ProgressListener {
    ProgressListener NONE = charactersRead -> {};

    void onCharactersRead(long charactersRead);

    default void clear() {}

    default void finish() {}
}
