package com.artixcloud.utils;

import com.artixcloud.ArtixCloud;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

/**
 * Utilitário para formatação e envio de mensagens com suporte a cores e placeholders.
 */
public final class MessageUtil {

    private static ArtixCloud plugin;
    private static FileConfiguration messages;
    private static String prefix;

    private MessageUtil() {}

    public static void load(ArtixCloud instance) {
        plugin = instance;
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
        prefix = color(messages.getString("prefix", "&8[&bArtix&3Cloud&8] &r"));
    }

    /** Colore uma string usando o símbolo &. */
    public static String color(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    /** Transforma uma string colorida em Component do Adventure. */
    public static Component component(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(color(text));
    }

    /** Obtém uma mensagem do messages.yml e substitui placeholders. */
    public static String get(String path, Map<String, String> replacements) {
        String msg = messages.getString(path, "&cMensagem não encontrada: " + path);
        msg = color(msg);
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return msg;
    }

    public static String get(String path) {
        return get(path, null);
    }

    /** Envia uma mensagem com prefixo ao jogador. */
    public static void send(Player player, String path, Map<String, String> replacements) {
        player.sendMessage(prefix + get(path, replacements));
    }

    public static void send(Player player, String path) {
        send(player, path, null);
    }

    /** Envia uma mensagem já formatada (sem buscar no messages.yml). */
    public static void sendRaw(Player player, String message) {
        player.sendMessage(prefix + color(message));
    }

    /** Envia uma linha de separador estilizado. */
    public static void sendSeparator(Player player) {
        player.sendMessage(color("&8&m" + "─".repeat(40)));
    }

    /** Formata um número de segundos como tempo legível. */
    public static String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public static String getPrefix() { return prefix; }
}
