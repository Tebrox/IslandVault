package de.tebrox.islandVault;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Base64ItemSerializer {

    public static String serializeItemStack(ItemStack item) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack deserialize(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("Base64-String ist null oder leer");
        }

        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Ungültiger Base64-String", e);
        }

        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            Object obj = in.readObject();
            if (obj instanceof ItemStack item) {
                return item;
            } else {
                throw new RuntimeException("Deserialisiertes Objekt ist kein ItemStack, sondern: " + obj.getClass());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Konnte ItemStack nicht deserialisieren", e);
        }
    }

}