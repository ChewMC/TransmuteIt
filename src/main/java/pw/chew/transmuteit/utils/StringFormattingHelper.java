package pw.chew.transmuteit.utils;

import java.util.ArrayList;
import java.util.List;

public class StringFormattingHelper {
    // Method to convert "WORD_WORD" to "Word Word"
    public static String capitalize(String to) {
        String[] words = to.split("_");
        if(words.length == 0) {
            return "";
        }
        List<CharSequence> output = new ArrayList<>();
        for (String word : words) {
            if(word.length() == 0) {
                output.add("");
            } else {
                String rest = word.substring(1).toLowerCase();
                String first = word.substring(0, 1).toUpperCase();
                output.add(first + rest);
            }
        }
        return String.join(" ", output);
    }
}
