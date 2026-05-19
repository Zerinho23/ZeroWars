package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import org.bukkit.entity.Player;

  /** Punto de entrada para abrir cualquier GUI de ZeroWars. */
  public class GuiManager {

      private final ZeroWars plugin;

      public GuiManager(ZeroWars plugin) { this.plugin = plugin; }

      public void openMainMenu(Player player)               { new MainMenuGui(plugin, player).open(); }
      public void openZoneList(Player player)               { new ZoneListGui(plugin, player, 0).open(); }
      public void openZoneDetail(Player player, String id)  { new ZoneDetailGui(plugin, player, id).open(); }
      public void openEventList(Player player)              { new EventListGui(plugin, player).open(); }
      public void openZoneCreate(Player player)             { new ZoneCreateGui(plugin, player).open(); }
      public void openRewardList(Player player)             { new RewardListGui(plugin, player, 0).open(); }
      public void openRewardDetail(Player player, String id){ new RewardDetailGui(plugin, player, id).open(); }
  }