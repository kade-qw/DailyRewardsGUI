package com.example.dailyrewardsgui.util;

public final class TimeUtil {

    private TimeUtil() {
        // Clase de utilidades: no se instancia
    }

    /**
     * Convierte milisegundos a formato HH:MM:SS.
     * Redondea hacia arriba para que nunca muestre 00:00:00 mientras aún queda tiempo.
     */
    public static String formatHMS(long millis) {
        long totalSeconds = (Math.max(0L, millis) + 999L) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}