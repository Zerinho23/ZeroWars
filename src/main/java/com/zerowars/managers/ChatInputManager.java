package com.zerowars.managers;

  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Bukkit;
  import org.bukkit.entity.Player;
  import org.bukkit.event.EventHandler;
  import org.bukkit.event.EventPriority;
  import org.bukkit.event.Listener;
  import org.bukkit.event.player.AsyncPlayerChatEvent;
  import org.bukkit.plugin.java.JavaPlugin;

  import java.util.Map;
  import java.util.UUID;
  import java.util.concurrent.ConcurrentHashMap;
  import java.util.function.BiConsumer;

  /**
   * Gestiona entradas de chat esperadas por GUIs.
   * Cuando un GUI necesita texto del jugador (nombre de zona, monto de reward),
   * registra un callback aqui. El siguiente mensaje del jugador dispara el callback
   * (cancelando el chat normal) y retoma el GUI en el hilo principal.
   */
  public class ChatInputManager implements Listener {

      @FunctionalInterface
      public interface InputCallback {
          void onInput(Player player, String input);
      }

      private final JavaPlugin plugin;
      private final Map<UUID, InputCallback> pending = new ConcurrentHashMap<>();

      public ChatInputManager(JavaPlugin plugin) { this.plugin = plugin; }

      /**
       * Espera el proximo mensaje de chat del jugador y lo entrega al callback.
       * @param player   jugador que debe escribir en chat
       * @param prompt   mensaje mostrado pidiendo la entrada (puede ser null)
       * @param callback logica a ejecutar con el texto ingresado (corre en hilo principal)
       */
      public void awaitInput(Player player, String prompt, InputCallback callback) {
          if (prompt != null) MessageUtil.send(player, prompt);
          pending.put(player.getUniqueId(), callback);
      }

      public boolean hasPending(UUID uuid) { return pending.containsKey(uuid); }

      public void cancel(UUID uuid) { pending.remove(uuid); }

      @EventHandler(priority = EventPriority.LOWEST)
      public void onChat(AsyncPlayerChatEvent e) {
          InputCallback cb = pending.remove(e.getPlayer().getUniqueId());
          if (cb == null) return;
          e.setCancelled(true);
          String input = e.getMessage().trim();
          Bukkit.getScheduler().runTask(plugin, () -> cb.onInput(e.getPlayer(), input));
      }
  }