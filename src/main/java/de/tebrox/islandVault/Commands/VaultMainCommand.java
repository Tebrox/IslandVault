package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Manager.CommandManager.argumentMatcher.ContainingAllCharsOfStringArgumentMatcher;
import de.tebrox.islandVault.Manager.VaultManager;

public class VaultMainCommand extends MainCommand {

    public VaultMainCommand() {
        super("§cYou don't have permission to do that.", new ContainingAllCharsOfStringArgumentMatcher());
    }

    @Override
    protected void registerSubCommands() {
        subCommands.add(new OpenCommand());
        subCommands.add(new HelpCommand());
        subCommands.add(new ItemSearchCommand());
        subCommands.add(new SettingsCommand());

    }
}
