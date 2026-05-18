package com.zerowars.placeholders;

import com.zerowars.ZeroWars;
import com.zerowars.models.PlayerData;
import com.zerowars.models.Zone;
import com.zerowars.utils.MessageUtil;
import com.zerowars.utils.RegionUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Expansión de PlaceholderAPI para ZeroWars.
 *
 * Placeholders disponibles:
 *   %zerowars_zone%              — Nombre de la zona actual del jugador
 *   %zerowars_zone_owner%        — Dueño de la zona actual
 *   %zerowars_zone_capture%      — Porcentaje de captura de la zona actual
 *   %zerowars_zone_level%        — Nivel de la zona actual
 *   %zerowars_zone_state%        — Estado de la zona (NEUTRAL/OWNED/CAPTURING/CONTESTED)
 *   %zerowars_heat%              — Nivel de heat del jugador
 *   %zerowars_heat_name%         — Nombre del nivel de heat
 *   %zerowars_kills%             — Kills del jugador
 *   %zerowars_deaths%            — Deaths del jugador
 *   %zerowars_captures%          — Capturas del jugador
 *   %zerowars_domine_time%       — Tiempo total dominado (formateado)
 *   %zerowars_kd%                — Ratio K/D del jugador
 *   %zerowars_clan%              — Nombre del clan del jugador
 *   %zerowars_cooldown_<id>%     — Segundos restantes del cooldown de un consumible
 *   %zerowars_top_kills_<pos>%   — Jugador en posición X del top kills
 *   %zerowars_top_captures_<pos>% — Jugador en posición X del top capturas
 *   %zerowars_events%            — Número de eventos activos
 */
public class ZeroWarsPlaceholders extends PlaceholderExpansion {

    private final ZeroWars plugin;

    public ZeroWarsPlaceholders(ZeroWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "zerowars"; }

    @Override
    public @NotNull String getAuthor() { return "zerinho23"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // ── Zona actual ──────────────────────────────────────────────────────
        if (params.equals("zone")) {
            return plugin.getZoneManager().getPlayerZone(player.getUniqueId())
                    .map(z -> MessageUtil.stripFormatting(z.getDisplayName()))
                    .orElse("Ninguna");
        }

        if (params.equals("zone_owner")) {
            return plugin.getZoneManager().getPlayerZone(player.getUniqueId())
                    .map(z -> z.isOwned() ? z.getOwnerName() : "Nadie")
                    .orElse("Nadie");
        }

        if (params.equals("zone_capture")) {
            return plugin.getZoneManager().getPlayerZone(player.getUniqueId())
                    .map(z -> String.format("%.1f", z.getCaptureProgress()))
                    .orElse("0.0");
        }

        if (params.equals("zone_level")) {
            return plugin.getZoneManager().getPlayerZone(player.getUniqueId())
                    .map(z -> String.valueOf(z.getLevel()))
                    .orElse("0");
        }

        if (params.equals("zone_state")) {
            return plugin.getZoneManager().getPlayerZone(player.getUniqueId())
                    .map(z -> z.getState().name())
                    .orElse("FUERA");
        }

        // ── Heat ─────────────────────────────────────────────────────────────
        if (params.equals("heat")) {
            return String.valueOf(plugin.getHeatManager().getHeatLevel(player.getUniqueId()));
        }

        if (params.equals("heat_name")) {
            int level = plugin.getHeatManager().getHeatLevel(player.getUniqueId());
            return level > 0 ? MessageUtil.stripFormatting(plugin.getHeatManager().getHeatName(level)) : "";
        }

        // ── Estadísticas del jugador ─────────────────────────────────────────
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());

        if (params.equals("kills"))      return data != null ? String.valueOf(data.getKills()) : "0";
        if (params.equals("deaths"))     return data != null ? String.valueOf(data.getDeaths()) : "0";
        if (params.equals("captures"))   return data != null ? String.valueOf(data.getCaptures()) : "0";
        if (params.equals("kd"))         return data != null ? String.format("%.2f", data.getKDRatio()) : "0.00";
        if (params.equals("domine_time")) {
            return data != null ? RegionUtil.formatTime(data.getTotalDomineTimeMs()) : "0s";
        }

        // ── Clan ──────────────────────────────────────────────────────────────
        if (params.equals("clan")) {
            String clanId = plugin.getClanManager().getClanOfPlayer(player.getUniqueId());
            return clanId != null ? plugin.getClanManager().getClanName(clanId) : "Sin clan";
        }

        // ── Cooldown de consumible: %zerowars_cooldown_<id>% ─────────────────
        if (params.startsWith("cooldown_")) {
            String consumableId = params.substring("cooldown_".length());
            if (data != null) {
                long remaining = data.getConsumableCooldownSeconds(consumableId);
                return remaining > 0 ? String.valueOf(remaining) : "0";
            }
            return "0";
        }

        // ── Ranking top kills: %zerowars_top_kills_1% ────────────────────────
        if (params.startsWith("top_kills_")) {
            try {
                int pos = Integer.parseInt(params.substring("top_kills_".length())) - 1;
                List<PlayerData> top = plugin.getRankingManager().getRanking("kills");
                if (pos >= 0 && pos < top.size()) return top.get(pos).getName();
            } catch (NumberFormatException ignored) {}
            return "N/A";
        }

        if (params.startsWith("top_captures_")) {
            try {
                int pos = Integer.parseInt(params.substring("top_captures_".length())) - 1;
                List<PlayerData> top = plugin.getRankingManager().getRanking("captures");
                if (pos >= 0 && pos < top.size()) return top.get(pos).getName();
            } catch (NumberFormatException ignored) {}
            return "N/A";
        }

        // ── Eventos activos ───────────────────────────────────────────────────
        if (params.equals("events")) {
            return String.valueOf(plugin.getEventManager().getActiveCount());
        }

        return null; // Placeholder no reconocido
    }
}
