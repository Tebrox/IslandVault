package de.tebrox.islandVault.enums;

public enum Permissions {

    VAULT("islandvault.vault."),
    GROUPS("islandvault.groups."),

    GROUPS_CONFIG("permission_groups");

    private String label;

    Permissions(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
