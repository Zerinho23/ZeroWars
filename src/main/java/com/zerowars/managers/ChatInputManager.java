package com.zerowars.managers;

import com.zerowars.utils.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona entradas de chat esperadas por GUIs.
 * FIX: migrado de AsyncPlayerChatEvent (deprecado en Paper 1.20.4+)
 *      a io.papermc.paper.event.player.AsyncChatEvent.
 */
public class ChatInputManager implements Listener {

    @FunctionalInterface
    public interface InputCallback {
        void onInput(Player player, String input);
    }

    private final JavaPlugin plugin;
    private final Map<UUID, InputCallback> pending = new ConcurrentHashMap<>();

    public ChatInputManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void awaitInput(Player player, String prompt, InputCallback callback) {
        if (prompt != null) MessageUtil.send(player, prompt);
        pending.put(player.getUniqueId(), callback);
    }

    public boolean hasPending(UUID uuid) { return pending.containsKey(uuid); }

    public void cancel(UUID uuid) { pending.remove(uuid); }

    /**
     * FIX: usa AsyncChatEvent (Paper 1.20.4+) en lugar del deprecado
     *      AsyncPlayerChatEvent. El texto se extrae con PlainTextComponentSerializer.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        InputCallback cb = pending.remove(e.getPlayer().getUniqueId());
        if (cb == null) return;
        e.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText()
                .serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> cb.onInput(e.getPlayer(), input));
    }
}
