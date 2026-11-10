/*
 * My Project
 */

package value;

import java.math.BigDecimal;
import java.math.MathContext;

public final class PrintScriptValues {
    public static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL64;

    private PrintScriptValues() {}

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
