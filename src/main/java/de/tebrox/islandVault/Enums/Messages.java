package de.tebrox.islandVault.Enums;

public enum Messages {
    MENU_TITLE("messages.menuTitle"),
    NO_PLAYER("messages.noPlayer"),
    NO_PERMISSION("messages.noPermission");

    private String label;

    Messages(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
