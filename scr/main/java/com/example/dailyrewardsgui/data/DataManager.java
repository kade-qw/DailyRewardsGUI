package com.example.dailyrewardsgui.data;

import com.example.dailyrewardsgui.DailyRewardsGUI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Guarda y consulta el momento (en milisegundos) del último reclamo de cada jugador.
 * Los datos viven en memoria (Map) y se persisten en data.yml usando el UUID como clave.
 */
public class DataManager {

    private final DailyRewardsGUI plugin;
    private final File file;
    private final Map<UUID, Long> lastClaims = new HashMap<>();
    private final Object fileLock = new Object();

    public DataManager(DailyRewardsGUI plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    /** Carga data.yml en memoria. Se llama una sola vez en onEnable. */
    public void load() {
        lastClaims.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("players")) {
            return;
        }
        for (String key : yaml.getConfigurationSection("players").getKeys(false)) {
            try {
                lastClaims.put(UUID.fromString(key), yaml.getLong("players." + key));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("UUID inválido en data.yml, se ignora: " + key);
            }
        }
    }

    /** Devuelve el timestamp del último reclamo, o 0 si nunca reclamó. */
    public long getLastClaim(UUID uuid) {
        return lastClaims.getOrDefault(uuid, 0L);
    }

    /** Milisegundos que le faltan al jugador para poder reclamar (0 = ya puede). */
    public long getRemainingMillis(UUID uuid, long cooldownMillis) {
        long last = getLastClaim(uuid);
        if (last == 0L) {
            return 0L;
        }
        long remaining = (last + cooldownMillis) - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    public boolean canClaim(UUID uuid, long cooldownMillis) {
        return getRemainingMillis(uuid, cooldownMillis) == 0L;
    }

    /** Registra un reclamo ahora mismo y lo guarda en disco sin bloquear el hilo principal. */
    public void setClaimedNow(UUID uuid) {
        lastClaims.put(uuid, System.currentTimeMillis());
        saveAsync();
    }

    /** Serializa en el hilo principal (el Map no es thread-safe) y escribe el archivo en otro hilo. */
    private void saveAsync() {
        final String snapshot = serialize();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(snapshot));
    }

    /** Guardado síncrono. Se usa en onDisable, donde ya no se pueden programar tareas. */
    public void saveSync() {
        write(serialize());
    }

    private String serialize() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : lastClaims.entrySet()) {
            yaml.set("players." + entry.getKey(), entry.getValue());
        }
        return yaml.saveToString();
    }

    private void write(String content) {
        synchronized (fileLock) {
            try {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo guardar data.yml", ex);
            }
        }
    }
}