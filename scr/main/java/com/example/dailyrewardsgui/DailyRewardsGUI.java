package com.example.dailyrewardsgui;

import com.example.dailyrewardsgui.data.DataManager;
import com.example.dailyrewardsgui.gui.DailyHolder;
import com.example.dailyrewardsgui.gui.GuiManager;
import com.example.dailyrewardsgui.listener.GuiListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class DailyRewardsGUI extends JavaPlugin implements CommandExecutor {

    private DataManager dataManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.dataManager = new DataManager(this);
        this.dataManager.load();

        this.guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        PluginCommand command = getCommand("daily");
        if (command == null) {
            getLogger().severe("El comando 'daily' no está en plugin.yml. Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);

        getLogger().info("DailyRewardsGUI habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        // Cierra los menús abiertos para que nadie se quede con una GUI "huérfana"
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof DailyHolder) {
                player.closeInventory();
            }
        }
        if (dataManager != null) {
            dataManager.saveSync();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {

        // /daily reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("dailyrewards.reload")) {
                sender.sendMessage(message("no-permission"));
                return true;
            }
            reloadConfig();
            sender.sendMessage(message("reloaded"));
            return true;
        }

        // /daily  o  /recompensa
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("only-players"));
            return true;
        }
        if (!player.hasPermission("dailyrewards.use")) {
            player.sendMessage(message("no-permission"));
            return true;
        }

        guiManager.open(player);
        return true;
    }

    // ---------- Utilidades compartidas con el resto del plugin ----------

    /** Convierte codigos & a colores de Minecraft. */
    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    /** Obtiene messages.<key> de la config con el prefijo ya aplicado. */
    public String message(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String text = getConfig().getString("messages." + key, "&cMensaje no encontrado: " + key);
        return color(prefix + text);
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}