package com.example.dailyrewardsgui.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marca nuestros inventarios. Asi el listener identifica el menu con un
 * "instanceof" en lugar de comparar titulos, que es fragil y facil de falsificar.
 * No guarda referencias a Player (solo el UUID), para no retener objetos en memoria.
 */
public class DailyHolder implements InventoryHolder {

    private Inventory inventory;

    /** Crea el inventario de cofre con este holder como dueño. */
    public Inventory create(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        return inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}