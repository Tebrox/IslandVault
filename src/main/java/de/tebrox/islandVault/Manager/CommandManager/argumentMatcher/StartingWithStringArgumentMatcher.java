package de.tebrox.islandVault.Manager.CommandManager.argumentMatcher;

import de.tebrox.islandVault.Manager.CommandManager.ArgumentMatcher;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ArgumentMatcher} that filters tab completions by checking
 * whether each suggestion starts with the input argument.
 * <p>
 * Example:
 * <ul>
 *     <li>Input: "he"</li>
 *     <li>Matches: "help", "hello", "heal"</li>
 *     <li>Non-Matches: "echo", "thelp", "cheat"</li>
 * </ul>
 * <p>
 * This matcher is ideal for traditional tab completion behavior
 * where suggestions start with what the user typed.
 */
public class StartingWithStringArgumentMatcher implements ArgumentMatcher {

    /**
     * Filters a list of tab completions by retaining only those that start
     * with the {@code argument} string.
     *
     * @param tabCompletions The list of candidate completions.
     * @param argument       The user's input string to match.
     * @return A filtered list containing only strings that start with {@code argument}.
     */
    @Override
    public List<String> filter(List<String> tabCompletions, String argument) {
        List<String> result = new ArrayList<>();

        StringUtil.copyPartialMatches(argument, tabCompletions, result);

        return result;
    }
}
