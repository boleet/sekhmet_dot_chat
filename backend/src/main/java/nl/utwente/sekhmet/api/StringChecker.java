package nl.utwente.sekhmet.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringChecker {

    public static final List<String> ESCAPE_CHARACTERS = Arrays.asList("\b", "\f", "\n", "\r", "\t", "\"", "\\");

    public static final List<String> ESCAPE_CHARACTERS_WITH_BSLASH = Arrays.asList("\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\\"");

    public static final List<String> FIELD_NAMES = Arrays.asList("\"content\":", "\"name\":");
    /*
    Need to find the field in JSON (such as the field "content" for message) while still in String format
        and then edit the escape characters inside the String value of the field. Converting to JSONObject before escaping will return errors.
    Finding the correct " (double quotes) to change inside the JSON string version is a pain in the butt,head,legs,etc.
     */

    public static String escapeString(String test, boolean checkAll) {
        if (checkAll) {
            StringBuilder result = new StringBuilder(test);
            int next = 0;
            int size = test.length();
            for (int i = 0; i < size + next; i++){
                String temp = result.substring(i, i+1);
                boolean check = ESCAPE_CHARACTERS.contains(temp);
                if (check) {
                    if (temp.contains("\\")) {
                        if (!ESCAPE_CHARACTERS_WITH_BSLASH.contains(result.substring(i, i+2))) {
                            result.insert(i, "\\");
                            i++;
                            next++;
                        }
                    } else if (temp.contains("\n")) {
                        result.replace(i,i+1, "\\n");
                        i++;
                        next++;
                    } else if (temp.contains("\r")){
                        result.replace(i,i+1, "\\r");
                        i++;
                        next++;
                    } else if (temp.contains("\f")){
                        result.replace(i,i+1, "\\f");
                        i++;
                        next++;
                    } else if (temp.contains("\b")){
                        result.replace(i,i+1, "\\b");
                        i++;
                        next++;
                    } else if (temp.contains("\"")) {
                        result.replace(i,i+1, "\\\"");
                        i++;
                        next++;
                    }
                }
            }
            return result.toString() + " (" + next +")";
        } else {
            return escapeString(test);
        }
    }
    public static String escapeString(String test) {
        List<Integer> endList = new ArrayList<>();
        for (String check: FIELD_NAMES) {
            Pattern word = Pattern.compile(check);
            Matcher match = word.matcher(test);
            while(match.find()) {
                endList.add(match.end());
            }
        }
        if (endList.isEmpty()) {
            return test;
        }
        Collections.sort(endList);
        StringBuilder result = new StringBuilder(test);

        int prev = 0;
        int end = 0;
        int next = 0;
        for (int index = 0; index < endList.size();index++) {
            end = endList.get(index) + prev;
            if (index != (endList.size() - 1)) {
                next = endList.get(index+1) + prev;
            } else {
                next = result.length();
            }
            boolean contentStartFound = false;
            boolean contentEndFound = false;
            for (int i = end; i < next; i++) {
                boolean contains = result.substring(i, i+1).contains("\"");
                if (contains && !contentStartFound) {
                    contentStartFound = true;
                    continue;
                } else if (contains && !contentEndFound) {
                    String substring = result.substring(i+1, i+2); //check here if escaping wrong characters
                    if (substring.contains("}") || substring.contains(",")){
                        contentEndFound = true;
                    }
                } else {
                    if (!contentStartFound && result.substring(i, i+1).contains("{")) {
                        i = next - 1;
                    }
                }

                if (contentStartFound) {
                    if (!contentEndFound) {
                        String temp = result.substring(i, i+1);
                        boolean check = ESCAPE_CHARACTERS.contains(temp);
                        if (check) {
                            if (temp.contains("\\")) {
                                if (!ESCAPE_CHARACTERS_WITH_BSLASH.contains(result.substring(i, i+2))) {
                                    result.insert(i, "\\");
                                    prev++;
                                    next++;
                                }
                                i++;
                            } else if (temp.contains("\n")) {
                                result.replace(i,i+1, "\\n");
                                prev++;
                                next++;
                                i++;
                            } else if (temp.contains("\r")){
                                result.replace(i,i+1, "\\r");
                                next++;
                                prev++;
                                i++;
                            } else if (temp.contains("\f")){
                                result.replace(i,i+1, "\\f");
                                next++;
                                prev++;
                                i++;
                            } else if (temp.contains("\b")){
                                result.replace(i,i+1, "\\b");
                                next++;
                                prev++;
                                i++;
                            } else if (temp.contains("\"")) {
                                result.replace(i,i+1, "\\\"");
                                prev++;
                                next++;
                                i++;
                            }
                        }
                    }
                }
            }
        }
        return result.toString();
    }
}
