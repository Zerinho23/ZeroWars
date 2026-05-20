package com.zerowars.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utilidad para parsear y enviar mensajes con soporte de:
 *   - MiniMessage nativo:            <gradient:#ff0000:#0000ff>Hola!</gradient>
 *   - Hex legacy CMI/Essentials:     &#ff4400Texto
 *   - Códigos legacy Bukkit:         &6Texto dorado   &l Negrita   &r Reset
 */
public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {}

    /**
     * Convierte códigos legacy al formato MiniMessage antes de parsear.
     *   &#RRGGBB  →  <color:#RRGGBB>
     *   &0-9      →  colores oscuros/claros
     *   &a-f/A-F  →  colores Bukkit
     *   &l/m/n/o/k/r  →  formatos
     */
    public static String translateColors(String text) {
        if (text == null) return "";
        // Hex &#RRGGBB → <color:#RRGGBB>
        text = text.replaceAll("(?i)&#([0-9a-f]{6})", "<color:#$1>");
        // Números
        text = text.replace("&0","<black>").replace("&1","<dark_blue>")
                   .replace("&2","<dark_green>").replace("&3","<dark_aqua>")
                   .replace("&4","<dark_red>").replace("&5","<dark_purple>")
                   .replace("&6","<gold>").replace("&7","<gray>")
                   .replace("&8","<dark_gray>").replace("&9","<blue>");
        // Letras (ambos casos)
        text = text.replace("&a","<green>").replace("&A","<green>")
                   .replace("&b","<aqua>").replace("&B","<aqua>")
                   .replace("&c","<red>").replace("&C","<red>")
                   .replace("&d","<light_purple>").replace("&D","<light_purple>")
                   .replace("&e","<yellow>").replace("&E","<yellow>")
                   .replace("&f","<white>").replace("&F","<white>");
        // Formatos
        text = text.replace("&l","<bold>").replace("&L","<bold>")
                   .replace("&m","<strikethrough>").replace("&M","<strikethrough>")
                   .replace("&n","<underlined>").replace("&N","<underlined>")
                   .replace("&o","<italic>").replace("&O","<italic>")
                   .replace("&k","<obfuscated>").replace("&K","<obfuscated>")
                   .replace("&r","<reset>").replace("&R","<reset>");
        return text;
    }

    // ── Parseo ───────────────────────────────────────────────────────────────

    public static Component parse(String message) {
        if (message == null) return Component.empty();
        return MM.deserialize(translateColors(message));
    }

    public static Component parse(String message, TagResolver... resolvers) {
        if (message == null) return Component.empty();
        return MM.deserialize(translateColors(message), resolvers);
    }

    // ── Envío ────────────────────────────────────────────────────────────────

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static void sendLines(CommandSender sender, String... lines) {
        for (String line : lines) sender.sendMessage(parse(line));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    public static void sendTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                parse(title), parse(subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn  * 50L),
                        java.time.Duration.ofMillis(stay    * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L))));
    }

    public static void broadcast(String message) {
        org.bukkit.Bukkit.broadcast(parse(message));
    }

    public static String serialize(Component component) { return MM.serialize(component); }

    public static String stripFormatting(String message) {
        if (message == null) return "";
        return MM.stripTags(translateColors(message));
    }

    public static Component separator(int width) {
        return parse("<dark_gray>" + "─".repeat(width));
    }
}
