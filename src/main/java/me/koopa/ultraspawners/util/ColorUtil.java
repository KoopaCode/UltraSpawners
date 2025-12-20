package me.koopa.ultraspawners.util;

public class ColorUtil {
    
    /**
     * Translates color codes from & to § format
     * @param text Text with & color codes
     * @return Text with § color codes
     */
    public static String color(String text) {
        if (text == null) return null;
        return text.replace('&', '§');
    }
}
