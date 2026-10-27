/*
 * My Project
 */

package value;

import java.math.BigDecimal;
import java.math.MathContext;

/** Arithmetic and textual representation rules for PrintScript runtime values. */
public final class PrintScriptValues {
    /** Precision used for divisions that do not terminate (e.g. 10 / 3). */
    public static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL64;

    private PrintScriptValues() {}

    /** Removes insignificant zeros so that {@code 1.5 * 2} is {@code 3}, as in TypeScript. */
    public static BigDecimal normalize(BigDecimal number) {
        BigDecimal stripped = number.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    public static String display(Object value) {
        if (value instanceof BigDecimal number) {
            return normalize(number).toPlainString();
        }
        return String.valueOf(value);
    }
}
