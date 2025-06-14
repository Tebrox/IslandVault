package de.tebrox.islandVault;

import de.tebrox.islandVault.Utils.ItemStackKey;

public class ItemPermissionRule {

    public enum RuleType {
        WHITELIST,
        BLACKLIST
    }

    private final ItemStackKey key;
    private final RuleType type;

    public ItemPermissionRule(ItemStackKey key, RuleType type) {
        this.key = key;
        this.type = type;
    }

    public ItemStackKey getKey() {
        return key;
    }

    public RuleType getType() {
        return type;
    }
}