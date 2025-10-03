package de.tebrox.islandVault.Enums;

public enum Permissions {

    COMMAND("islandvault.command."),
    VAULT("islandvault.vault."),
    GROUPS("islandvault.groups."),
    COLLECT_RADIUS("islandvault.collectradius."),

    OPEN_GUI("islandvault.command.openGUI"),
    OPEN_SETTINGS("islandvault.command.openSettings"),
    HELP("islandvault.command.help"),
    USE_SEARCH("islandvault.command.useSearch"),
    USE_BACKPACK("islandvault.command.useBackpack"),

    ADMIN_MAIN("islandvault.command.admin"),
    ADMIN_HELP("islandvault.command.admin.help"),
    ADMIN_OPEN_PLAYER_VAULT("islandvault.command.admin.openPlayerVault"),
    ADMIN_RELOAD_CONFIG("islandvault.command.admin.reloadConfig"),
    ADMIN_RELOAD_LANGUAGE("islandvault.command.admin.reloadLanguage"),
    ADMIN_USE_DEBUGGER("islandvault.command.admin.useDebugger"),
    ADMIN_SET_DEBUGMODE("islandvault.command.admin.setDebugMode"),

    GROUPS_CONFIG("permission_groups"),
    MESSAGE_ITEMLORE("messages.itemLore");

    private String label;

    Permissions(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
