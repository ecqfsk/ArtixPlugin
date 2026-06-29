package com.artixcloud.utils;

/**
 * Utilitários de cor e formatação de texto para interfaces do ArtixCloud.
 */
public final class ColorUtil {

    private ColorUtil() {}

    /** Converte caracteres & em §. */
    public static String translate(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    /** Remove todos os códigos de cor de uma string. */
    public static String strip(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    /**
     * Gera uma barra de progresso colorida.
     *
     * @param current valor atual
     * @param max     valor máximo
     * @param length  comprimento em caracteres
     * @param filled  cor da parte preenchida (ex: "&a")
     * @param empty   cor da parte vazia (ex: "&8")
     */
    public static String progressBar(double current, double max, int length, String filled, String empty) {
        if (max <= 0) return empty + "▌".repeat(length);
        int filledCount = (int) Math.round((current / max) * length);
        filledCount = Math.max(0, Math.min(length, filledCount));

        return translate(filled) + "▌".repeat(filledCount)
             + translate(empty)  + "▌".repeat(length - filledCount);
    }

    /**
     * Cor baseada em percentual (verde → amarelo → vermelho).
     */
    public static String percentColor(double percent) {
        if (percent < 50) return "&a";
        if (percent < 80) return "&e";
        return "&c";
    }
}
