package de.tebrox.islandVault.Enums;

public enum Commands {
    MAIN_COMMAND("/insellager"),
    MAIN_ADMIN_COMMAND("/insellageradmin");

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
