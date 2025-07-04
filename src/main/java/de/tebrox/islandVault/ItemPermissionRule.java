package de.tebrox.islandVault;

import de.tebrox.islandVault.Utils.ItemStackKey;

public class ItemPermissionRule {

    public enum RuleType {
        WHITELIST,
        BLACKLIST
    }

    private final String id;
    private final ItemStackKey key;
    private final RuleType type;

    public ItemPermissionRule(String id, ItemStackKey key, RuleType type) {
        this.id = id;
        this.key = key;
        this.type = type;
    }

    public ItemStackKey getKey() {
        return key;
    }

    public RuleType getType() {
        return type;
    }

    public String getId() { return id; }
}