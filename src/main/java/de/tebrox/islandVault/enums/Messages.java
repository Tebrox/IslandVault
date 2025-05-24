package de.tebrox.islandVault.enums;

public enum Messages {
    MENU_TITLE("messages.menuTitle"),
    NO_PLAYER("messages.noPlayer");

    private String label;

    Messages(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
