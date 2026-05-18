package com.zerowars.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utilidad para parsear y enviar mensajes con MiniMessage.
 * Soporta colores HEX, gradientes y todos los tags de MiniMessage.
 *
 * Uso:
 *   MessageUtil.send(player, "<gradient:#ff0000:#0000ff>Hola!</gradient>");
 *   MessageUtil.parse("<red>Texto rojo");
 */
public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {}

    /**
     * Parsea un string MiniMessage a Component de Adventure.
     */
    public static Component parse(String message) {
        if (message == null) return Component.empty();
        return MM.deserialize(message);
    }

    /**
     * Parsea con resolvers adicionales (para placeholders custom).
     */
    public static Component parse(String message, TagResolver... resolvers) {
        if (message == null) return Component.empty();
        return MM.deserialize(message, resolvers);
    }

    /**
     * Envía un mensaje parseado a un CommandSender (jugador o consola).
     */
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    /**
     * Envía múltiples mensajes línea por línea.
     */
    public static void sendLines(CommandSender sender, String... lines) {
        for (String line : lines) {
            sender.sendMessage(parse(line));
        }
    }

    /**
     * Envía un ActionBar a un jugador.
     */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    /**
     * Envía un Title al jugador.
     * @param title    Título principal
     * @param subtitle Subtítulo
     * @param fadeIn   Ticks de fade in
     * @param stay     Ticks que permanece
     * @param fadeOut  Ticks de fade out
     */
    public static void sendTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                parse(title),
                parse(subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    /**
     * Broadcast a todos los jugadores online.
     */
    public static void broadcast(String message) {
        org.bukkit.Bukkit.broadcast(parse(message));
    }

    /**
     * Serializa un Component de vuelta a string MiniMessage (útil para debug).
     */
    public static String serialize(Component component) {
        return MM.serialize(component);
    }

    /**
     * Elimina todas las tags MiniMessage de un string (texto plano).
     */
    public static String stripFormatting(String message) {
        return MM.stripTags(message);
    }

    /**
     * Crea un separator visual para listas/menús.
     */
    public static Component separator(int width) {
        return parse("<dark_gray>" + "─".repeat(width));
    }
}
