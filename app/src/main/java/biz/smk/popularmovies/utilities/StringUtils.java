/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.utilities;

import java.util.List;

/**
 * Helpers for string manipulation.
 */
public class StringUtils {

    /**
     * Concatenates strings by putting a "glue" string between them.
     *
     * @param glue The string to add between the concatenated string elements.
     * @param pieces The string elements to concatenate.
     * @return The concatenated string (with "glue" between the elements).
     */
    public static String join(String glue, List<String> pieces) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (String piece : pieces) {
            if (first) {
                first = false;
            } else {
                sb.append(glue);
            }
            sb.append(piece);
        }

        return sb.toString();
    }

    /**
     * Replaces all spaces with non-breaking spaces.
     *
     * @param string String to make "unbreakable".
     * @return The "unbreakable" string.
     */
    public static String makeStringUnbreakable(String string) {
        return string.replaceAll(" ", "\u00A0");
    }

    /**
     * Removes all carriage return characters (\r) from the given String.
     *
     * @param string The string to remove carriage returns from.
     * @return String without carriage returns.
     */
    public static String removeCarriageReturns(String string) {
        return string.replaceAll("\\r", "");
    }

}
