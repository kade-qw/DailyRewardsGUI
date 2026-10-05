package com.example.dailyrewardsgui.util;

import com.example.dailyrewardsgui.DailyRewardsGUI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemBuilder {

    private ItemBuilder() {
        // Clase de utilidades: no se instancia
    }

    /**
     * Construye un item a partir de una seccion del config.yml con las claves:
     * material, name, lore (lista) y glow (boolean).
     *
     * @param placeholders pares "%clave%" -> valor que se reemplazan en nombre y lore (puede ser null)
     */
    public static ItemStack fromSection(DailyRewardsGUI plugin, ConfigurationSection section,
                                        Map<String, String> placeholders) {
        if (section == null) {
            plugin.getLogger().warning("Falta una seccion de item en config.yml, se usa BARRIER.");
            return new ItemStack(Material.BARRIER);
        }

        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("Material invalido '" + materialName
                    + "' en " + section.getCurrentPath() + ", se usa STONE.");
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(DailyRewardsGUI.color(apply(section.getString("name", " "), placeholders)));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(DailyRewardsGUI.color(apply(line, placeholders)));
        }
        meta.setLore(lore);

        if (section.getBoolean("glow", false)) {
            applyGlow(meta);
        }

        item.setItemMeta(meta);
        return item;
    }

    /** item simple (sin lore ni brillo), usado para el relleno decorativo. */
    public static ItemStack simple(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(DailyRewardsGUI.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Añade un encantamiento oculto para que el item brille sin mostrar texto de encantamiento. */
    private static void applyGlow(ItemMeta meta) {
        // Se busca por clave porque el nombre de la constante cambió entre versiones (DURABILITY -> UNBREAKING)
        Enchantment glow = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (glow != null) {
            meta.addEnchant(glow, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    private static String apply(String text, Map<String, String> placeholders) {
        if (text == null) {
            return "";
        }
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace(entry.getKey(), entry.getValue());
            }
        }
        return text;
    }
}