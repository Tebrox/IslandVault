package de.tebrox.islandVault.Enums;

public enum Commands {
    MAIN_COMMAND("/islandvault");

    private final String label;

    Commands(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
