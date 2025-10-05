package de.tebrox.islandVault.Enums;

public enum Prefix {
    MAIN("[IslandVault]"),
    ITEMGROUPCONVERTER(MAIN + "[ItemGroupConverter]");


    private final String label;

    Prefix(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
