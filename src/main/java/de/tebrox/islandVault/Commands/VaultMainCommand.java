package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.CommandManager.argumentMatcher.ContainingAllCharsOfStringArgumentMatcher;

import java.util.logging.Level;

public class VaultMainCommand extends MainCommand {

    public VaultMainCommand() {
        super("§cYou don't have permission to do that.", new ContainingAllCharsOfStringArgumentMatcher());
    }

    @Override
    protected void registerSubCommands() {
        subCommands.add(new OpenCommand());
        subCommands.add(new HelpCommand());
        //subCommands.add(new ItemSearchCommand());
        //subCommands.add(new SettingsCommand());

        for(SubCommand cmd : getSubCommands()) {
            IslandVault.getPlugin().getVaultLogger().log(Level.INFO, "Registered command: " + cmd.getSyntax());
        }

    }
}
