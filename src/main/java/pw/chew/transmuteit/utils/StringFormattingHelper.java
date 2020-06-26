package pw.chew.transmuteit.utils;

public class StringFormattingHelper {
    // Method to convert "WORD_WORD" to "Word Word"
    public static String capitalize(String to) {
        String[] words = to.split("_");
        String newword = "";
        for (String word : words) {
            String rest = word.substring(1).toLowerCase();
            String first = word.substring(0, 1).toUpperCase();
            newword = newword + first + rest + " ";
        }
        return newword.substring(0, newword.length()-1);
    }
}
