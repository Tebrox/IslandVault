package de.tebrox.islandVault.Manager.CommandManager.argumentMatcher;

import de.tebrox.islandVault.Manager.CommandManager.ArgumentMatcher;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class StartingWithStringArgumentMatcher implements ArgumentMatcher {

    @Override
    public List<String> filter(List<String> tabCompletions, String argument) {
        List<String> result = new ArrayList<>();

        StringUtil.copyPartialMatches(argument, tabCompletions, result);

        return result;
    }
}
