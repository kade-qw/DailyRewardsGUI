package com.example.dailyrewardsgui.listener;

import com.example.dailyrewardsgui.DailyRewardsGUI;
import com.example.dailyrewardsgui.gui.DailyHolder;
import com.example.dailyrewardsgui.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class GuiListener implements Listener {

    private final DailyRewardsGUI plugin;

    public GuiListener(DailyRewardsGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof DailyHolder)) {
            return;
        }

        // Cancelamos SIEMPRE: evita sacar ítems, shift-click, teclas numéricas, doble clic, etc.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Solo reaccionamos a clics dentro del menú (no en el inventario del jugador)
        if (event.getClickedInventory() != top) {
            return;
        }
        if (event.getRawSlot() != plugin.getGuiManager().getClaimSlot()) {
            return;
        }

        handleClaim(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof DailyHolder) {
            event.setCancelled(true);
        }
    }

    private void handleClaim(Player player) {
        if (!player.hasPermission("dailyrewards.use")) {
            player.sendMessage(plugin.message("no-permission"));
            return;
        }

        UUID uuid = player.getUniqueId();
        long cooldown = plugin.getGuiManager().getCooldownMillis();
        long remaining = plugin.getDataManager().getRemainingMillis(uuid, cooldown);

        if (remaining > 0L) {
            String text = plugin.message("on-cooldown").replace("%time%", TimeUtil.formatHMS(remaining));
            player.sendMessage(text);
            playSound(player, "sounds.deny");
            return;
        }

        // Se registra ANTES de dar la recompensa: si el jugador hace clic muy rápido,
        // el segundo clic ya verá el cooldown activo y no se duplica el premio.
        plugin.getDataManager().setClaimedNow(uuid);

        for (String raw : plugin.getConfig().getStringList("rewards.commands")) {
            String command = raw.replace("%player%", player.getName()).trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (command.isEmpty()) {
                continue;
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception ex) {
                plugin.getLogger().warning("Error ejecutando el comando de recompensa '" + command + "': " + ex.getMessage());
            }
        }

        player.sendMessage(plugin.message("claimed"));
        playSound(player, "sounds.claim");

        // Se reabre el menú en el siguiente tick para mostrar el estado actualizado.
        // Se vuelve a buscar al jugador por UUID por si se desconecta en ese tick.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()) {
                plugin.getGuiManager().open(online);
            }
        });
    }

    /** Reproduce el sonido configurado en config.yml (ej. ENTITY_PLAYER_LEVELUP). */
    private void playSound(Player player, String path) {
        String name = plugin.getConfig().getString(path);
        if (name == null || name.isBlank()) {
            return;
        }
        String key = name.toLowerCase().replace('_', '.');
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } else {
            plugin.getLogger().warning("Sonido inválido en config.yml (" + path + "): " + name);
        }
    }
}