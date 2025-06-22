package de.tebrox.islandVault.Manager.CommandManager.argumentMatcher;

import de.tebrox.islandVault.Manager.CommandManager.ArgumentMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ArgumentMatcher} that filters tab completions by checking whether
 * each suggestion contains all characters from the provided input string,
 * in any order and position.
 * <p>
 * Example:
 * <ul>
 *     <li>Input: "abc"</li>
 *     <li>Matches: "cabbage", "abc", "bac"</li>
 *     <li>Non-Matches: "ab", "xyz", "def"</li>
 * </ul>
 * <p>
 * Useful for fuzzy matching or loose search-like tab completion behavior.
 */
public class ContainingAllCharsOfStringArgumentMatcher implements ArgumentMatcher {

    /**
     * Filters a list of tab completions by retaining only those that contain
     * all characters in the argument string, regardless of order.
     *
     * @param tabCompletions The list of strings to filter
     * @param argument       The user input string
     * @return A list of strings from {@code tabCompletions} that match the rule
     */
    @Override
    public List<String> filter(List<String> tabCompletions, String argument) {
        List<String> result = new ArrayList<>();

        for (String tabCompletion : tabCompletions) {
            boolean passes = true;

            for (char c : argument.toCharArray()) {
                passes = tabCompletion.contains(String.valueOf(c));

                if (!passes)
                    break;
            }

            if (passes) {
                result.add(tabCompletion);
            }

        }
        return result;
    }
}
