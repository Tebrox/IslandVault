package de.tebrox.islandVault;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Base64ItemSerializer {

    public static String serialize(ItemStack item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out)) {
            dataOut.writeObject(item);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize item", e);
        }
    }

    public static ItemStack deserialize(String base64) {
        byte[] data = Base64.getDecoder().decode(base64);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            return (ItemStack) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Could not deserialize item", e);
        }
    }
}