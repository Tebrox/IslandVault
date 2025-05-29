package de.tebrox.islandVault.Enums;

public enum Permissions {

    COMMAND("islandvault.command."),
    VAULT("islandvault.vault."),
    GROUPS("islandvault.groups."),
    COLLECT_RADIUS("islandvault.collectradius."),

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
