package de.tebrox.islandVault.Enums;

public enum Permissions {

    COMMAND("islandvault.cmd."),
    VAULT("islandvault.vault."),
    GROUPS("islandvault.groups."),
    CAN_OPEN_MENU("islandvault.canVaultOpen"),

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
