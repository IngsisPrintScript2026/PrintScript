/*
 * My Project
 */

package progress;

/** Observer notified while the source is consumed, so the CLI can show parsing progress. */
public interface ProgressListener {
    ProgressListener NONE = charactersRead -> {};

    void onCharactersRead(long charactersRead);

    /** Removes any progress currently drawn, so regular output is not mixed with it. */
    default void clear() {}

    /** Called once the whole source was processed. */
    default void finish() {}
}
