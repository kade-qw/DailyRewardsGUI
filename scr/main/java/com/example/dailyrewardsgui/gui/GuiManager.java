package com.example.dailyrewardsgui.gui;

import com.example.dailyrewardsgui.DailyRewardsGUI;
import com.example.dailyrewardsgui.util.ItemBuilder;
import com.example.dailyrewardsgui.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Construye y abre el menú de 27 slots. Solo lee datos y arma ítems:
 * no guarda referencias a jugadores ni a inventarios.
 */
public class GuiManager {

    private static final int SIZE = 27; // 3 filas

    private final DailyRewardsGUI plugin;

    public GuiManager(DailyRewardsGUI plugin) {
        this.plugin = plugin;
    }

    /** Abre el menú al jugador con el estado actual de su recompensa. */
    public void open(Player player) {
        String title = DailyRewardsGUI.color(plugin.getConfig().getString("gui.title", "&6Recompensa Diaria"));
        Inventory inventory = new DailyHolder().create(SIZE, title);

        fillBackground(inventory);
        inventory.setItem(getClaimSlot(), buildClaimItem(player.getUniqueId()));

        player.openInventory(inventory);
    }

    /** Slot del ítem central. Si el valor del config es inválido, usa el 13 (centro). */
    public int getClaimSlot() {
        int slot = plugin.getConfig().getInt("gui.claim-slot", 13);
        return (slot >= 0 && slot < SIZE) ? slot : 13;
    }

    /** Milisegundos de cooldown leídos del config (por defecto 24 h). */
    public long getCooldownMillis() {
        long seconds = plugin.getConfig().getLong("settings.cooldown-seconds", 86400L);
        return Math.max(1L, seconds) * 1000L;
    }

    /** Elige entre el ítem "disponible" o "no disponible" según el cooldown. */
    private ItemStack buildClaimItem(UUID uuid) {
        long remaining = plugin.getDataManager().getRemainingMillis(uuid, getCooldownMillis());

        if (remaining <= 0L) {
            return ItemBuilder.fromSection(plugin,
                    plugin.getConfig().getConfigurationSection("gui.items.available"), null);
        }

        Map<String, String> placeholders = Map.of("%time%", TimeUtil.formatHMS(remaining));
        return ItemBuilder.fromSection(plugin,
                plugin.getConfig().getConfigurationSection("gui.items.unavailable"), placeholders);
    }

    /** Rellena todos los slots con el cristal decorativo, si está activado en el config. */
    private void fillBackground(Inventory inventory) {
        if (!plugin.getConfig().getBoolean("gui.filler.enabled", true)) {
            return;
        }
        Material material = Material.matchMaterial(
                plugin.getConfig().getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (material == null || material.isAir()) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack filler = ItemBuilder.simple(material, plugin.getConfig().getString("gui.filler.name", " "));
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }
}