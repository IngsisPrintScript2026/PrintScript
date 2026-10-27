/*
 * My Project
 */

package progress;

import java.io.PrintStream;

/** Draws "Parsing: NN%" on a single, constantly rewritten line of the given stream. */
public final class ConsoleProgressReporter implements ProgressListener {
    private static final String LABEL = "Parsing: ";
    private static final int MAX_PERCENT = 100;

    private final PrintStream out;
    private final long totalCharacters;
    private int lastPercent = -1;
    private boolean drawn;

    public ConsoleProgressReporter(PrintStream out, long totalCharacters) {
        this.out = out;
        this.totalCharacters = Math.max(1, totalCharacters);
    }

    @Override
    public void onCharactersRead(long charactersRead) {
        int percent = (int) Math.min(MAX_PERCENT, charactersRead * MAX_PERCENT / totalCharacters);
        if (percent != lastPercent || !drawn) {
            draw(percent);
        }
    }

    @Override
    public void clear() {
        if (drawn) {
            out.print("\r" + " ".repeat(LABEL.length() + 4) + "\r");
            out.flush();
            drawn = false;
        }
    }

    @Override
    public void finish() {
        if (lastPercent >= 0) {
            draw(MAX_PERCENT);
            out.println();
            drawn = false;
        }
    }

    private void draw(int percent) {
        lastPercent = percent;
        drawn = true;
        out.print("\r" + LABEL + percent + "%");
        out.flush();
    }
}
