package de.tebrox.islandVault.Update;

public class VersionComparator {
    public static int compareVersions(String v1, String v2) {
        v1 = v1.replace("v", "");
        v2 = v2.replace("v", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseNumber(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseNumber(parts2[i]) : 0;

            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }

        return 0;
    }

    private static int parseNumber(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
