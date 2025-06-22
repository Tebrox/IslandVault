package de.tebrox.islandVault.Manager.CommandManager.argumentMatcher;

import de.tebrox.islandVault.Manager.CommandManager.ArgumentMatcher;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An {@link ArgumentMatcher} that filters tab completions by checking whether
 * each suggestion contains the full argument string as a substring.
 * <p>
 * This is a case-sensitive matcher.
 * <p>
 * Example:
 * <ul>
 *     <li>Input: "test"</li>
 *     <li>Matches: "mytestvalue", "test123", "123test456"</li>
 *     <li>Non-Matches: "tset", "tst", "exam"</li>
 * </ul>
 * <p>
 * Useful for simple, direct matching where the input must appear
 * as a continuous substring within the suggestion.
 */
public class ContainingStringArgumentMatcher implements ArgumentMatcher {

    /**
     * Filters a list of tab completions by retaining only those that contain
     * the full {@code argument} string as a substring.
     *
     * @param tabCompletions The list of candidate completions
     * @param argument       The user input string to match
     * @return A filtered list of strings containing {@code argument}
     */
    @Override
    public List<String> filter (List<String> tabCompletions, String argument) {
        return tabCompletions.stream().filter(tabCompletion -> tabCompletion.contains(argument)).collect(Collectors.toList());
    }
}
