# ZeroWars

  <div align="center">

  ![ZeroWars](https://img.shields.io/badge/ZeroWars-v1.2.0-red?style=for-the-badge&logo=minecraft)
  ![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk)
  ![Paper](https://img.shields.io/badge/Paper-1.20.4+-blue?style=for-the-badge)
  ![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
  ![Build](https://img.shields.io/github/actions/workflow/status/Zerinho23/ZeroWars/build-release.yml?branch=main&style=for-the-badge&label=Build)

  **Plugin competitivo de control de zonas PvP para Paper 1.20.4+**

  *Domina zonas, captura minas, destruye enemigos. Sin pausas. Sin tregua.*

  [Descargar v1.2.0](https://github.com/Zerinho23/ZeroWars/releases/latest) · [Issues](https://github.com/Zerinho23/ZeroWars/issues)

  </div>

  ---

  ## Que es ZeroWars?

  ZeroWars es un plugin **profesional, escalable y totalmente optimizado** para servidores Minecraft Paper enfocado en modalidades **BoxPvP**, **FullPvP** y PvP competitivo moderno.

  Los jugadores y clanes pelean por el control de zonas estrategicas: minas, rooms, areas raras y arenas de evento. El sistema esta disenado para mantener **accion constante**, rivalidades permanentes y recompensas reales.

  ---

  ## Caracteristicas

  ### Sistema de Zonas PvP
  - Tipos: `MINE`, `ROOM`, `RARE`, `EVENT`, `SPECIAL`
  - Cada zona tiene nombre, nivel (1-5), dueno, estado y recompensas configurables
  - **Sistema de nivel de zona**: al acumular tiempo de dominio la zona sube de nivel con bonus de recompensas
  - Particulas de borde y BossBar con gradientes (Adventure API, MiniMessage nativo)

  ### Sistema de Captura
  - **BossBar** con progreso y gradientes MiniMessage correctamente renderizados (Adventure API)
  - **ActionBar** con tiempo restante en tiempo real
  - Sistema de **contestacion**: defensores pausan o frenan la barra
  - Multiples atacantes aceleran la captura (configurable)
  - Cancelacion automatica si el atacante abandona la zona

  ### Sistema de Recompensas (v1.2.0)
  - **On-capture**: dinero + items + comandos al capturar
  - **Pasivas**: recompensas periodicas al dueno mientras domina (intervalo configurable por zona)
  - **Level-up**: bonus al subir de nivel la zona
  - Multiplicadores de evento aplicados automaticamente
  - **Vault**: economia compatible con EssentialsX, CMI y cualquier plugin estandar

  | Tipo | Cuando | Soporta |
  |------|--------|---------|
  | On-capture | Al tomar la zona | dinero, items, comandos |
  | Pasiva | Cada N segundos | dinero, items, intervalo |
  | Level-up | Al subir de nivel | bonus money, items por nivel |

  ### Consumibles PvP
  | Consumible | Efecto | Cooldown |
  |------------|--------|----------|
  | Boost de Velocidad | Speed III 10s | 30s |
  | Golpe Brutal | Fuerza II 8s | 45s |
  | Escudo Gravitacional | Resistencia IV 6s | 40s |
  | Curacion de Emergencia | +6 corazones + Regen | 60s |
  | Dash | Lanzamiento explosivo | 25s |
  | Escudo de Resistencia | Resistencia max 5s | 90s |
  | Sello de Vampiro | Lifesteal 8s | 75s |

  Activacion: **click derecho** sobre el item. Sin comandos ni chat.

  ### Sistema Heat / Wanted
  - 3 niveles de amenaza segun capturas acumuladas
  - Nivel maximo: el jugador brilla (Glow) y aparece en anuncio global
  - Multiplicador de recompensa al eliminar jugadores con heat alto

  ### Eventos Automaticos
  - Doble Loot, Mina Legendaria, Zona de Oro y mas (configurables)
  - Scheduler con intervalo y pool aleatorio configurables
  - Multiplicadores de recompensa globales por evento
  - Control manual: `/zw startevent <id>` y `/zw stopevent <id>`

  ### Ranking y Clanes
  - Top por kills, capturas y tiempo dominado
  - Soporte de clanes para dominio grupal de zonas
  - PlaceholderAPI con 15+ placeholders

  ---

  ## Base de Datos

  ### SQLite (por defecto)
  ```yaml
  database:
    type: sqlite
    file: "zerowars.db"
  ```

  ### MySQL / MariaDB
  ```yaml
  database:
    type: mysql
    mysql:
      host: "localhost"
      port: 3306
      database: "zerowars"
      username: "usuario"
      password: "contrasena"
      ssl: false
  ```

  ---

  ## Instalacion

  1. Descarga el `.jar` de los [Releases](https://github.com/Zerinho23/ZeroWars/releases/latest)
  2. Copialo a `plugins/`
  3. (Opcional) instala **Vault** + plugin de economia para recompensas de dinero
  4. Reinicia el servidor — el plugin genera todos los configs
  5. Edita `plugins/ZeroWars/config.yml`, `zones.yml`, `rewards.yml`
  6. Usa `/zw reload` para aplicar cambios sin reiniciar

  **Requisitos:** Paper 1.20.4+ · Java 17+ · PlaceholderAPI (opcional) · Vault (opcional)

  ---

  ## Comandos

  ### Jugador
  | Comando | Descripcion | Permiso |
  |---------|-------------|---------|
  | `/zone` | Info de la zona actual | `zerowars.player` |
  | `/zones` | Lista todas las zonas | `zerowars.player` |
  | `/top` | Ranking | `zerowars.player` |
  | `/events` | Eventos activos | `zerowars.player` |

  ### Administrador (`/zw`)
  | Comando | Descripcion |
  |---------|-------------|
  | `/zw reload` | Recarga toda la configuracion |
  | `/zw createzone <id> [tipo]` | Crea zona en tu posicion |
  | `/zw deletezone <id>` | Elimina una zona |
  | `/zw setreward <zona> <reward> [remove]` | Asigna o quita un reward a una zona |
  | `/zw giveconsumable <player> <id> [cant]` | Da consumibles |
  | `/zw startevent <id>` | Inicia un evento manualmente |
  | `/zw stopevent <id>` | Detiene un evento activo |
  | `/zw debug` | Toggle de logs de debug + estado del sistema |

  ---

  ## Permisos

  | Permiso | Default | Descripcion |
  |---------|---------|-------------|
  | `zerowars.admin` | OP | Acceso admin completo (incluye todos los sub-permisos) |
  | `zerowars.admin.reload` | OP | Recargar config |
  | `zerowars.admin.createzone` | OP | Crear zonas |
  | `zerowars.admin.deletezone` | OP | Eliminar zonas |
  | `zerowars.admin.setreward` | OP | Asignar rewards a zonas |
  | `zerowars.admin.giveconsumable` | OP | Dar consumibles |
  | `zerowars.admin.startevent` | OP | Iniciar eventos |
  | `zerowars.admin.stopevent` | OP | Detener eventos |
  | `zerowars.admin.debug` | OP | Modo debug |
  | `zerowars.bypass` | OP | Bypass de cooldowns y restricciones |
  | `zerowars.player` | true | Acceso a comandos de jugador |
  | `zerowars.zone.capture` | true | Capturar zonas |
  | `zerowars.consumable.use` | true | Usar consumibles PvP |

  ---

  ## PlaceholderAPI

  | Placeholder | Descripcion |
  |-------------|-------------|
  | `%zerowars_zone%` | Zona actual del jugador |
  | `%zerowars_zone_owner%` | Dueno de la zona actual |
  | `%zerowars_zone_capture%` | % de captura de la zona |
  | `%zerowars_zone_level%` | Nivel de la zona actual |
  | `%zerowars_zone_state%` | Estado (NEUTRAL/OWNED/CAPTURING/CONTESTED) |
  | `%zerowars_heat%` | Nivel de heat del jugador |
  | `%zerowars_kills%` | Kills del jugador |
  | `%zerowars_captures%` | Capturas del jugador |
  | `%zerowars_domine_time%` | Tiempo total dominado |
  | `%zerowars_kd%` | Ratio K/D |
  | `%zerowars_clan%` | Clan del jugador |
  | `%zerowars_cooldown_<id>%` | Segundos de cooldown de un consumible |
  | `%zerowars_top_kills_<pos>%` | Jugador #N en kills |
  | `%zerowars_top_captures_<pos>%` | Jugador #N en capturas |
  | `%zerowars_events%` | Numero de eventos activos |

  ---

  ## Configuracion rapida de rewards

  ```yaml
  # rewards.yml
  rewards:
    mi_reward:
      on-capture:
        money: 500
        items:
          1:
            material: DIAMOND
            amount: 2
        commands:
          - "say %player% capturo una zona!"
      passive:
        interval: 60   # segundos entre cada reward pasivo
        money: 100
      per-level:
        money-multiplier: 1.5  # bonus del 50% en on-capture por nivel
  ```

  ```yaml
  # zones.yml
  zones:
    mi_zona:
      rewards:
        - mi_reward
  ```

  ---

  ## Changelog

  ### v1.2.0
  - **feat:** `RewardManager.java` — sistema completo de recompensas implementado
    - On-capture: dinero, items y comandos al completar la captura
    - Pasivas: recompensas periodicas al dueno con intervalo configurable por zona
    - Level-up: bonus al subir de nivel la zona
    - Multiplicadores de evento integrados automaticamente
  - **feat:** Integracion **Vault** para recompensas de economia (softdep, funciona sin Vault)
  - **feat:** Sistema de **nivel de zona** — sube de nivel segun tiempo de dominio acumulado
    - Configurable: `zones.level-up.domine-minutes-per-level` en config.yml
  - **feat:** Subcomando `/zw setreward <zona> <reward> [remove]`
  - **feat:** Tab-completion completo en `/zw` (zonas, rewardIds, eventos, jugadores)
  - **feat:** `/zw debug` muestra estado completo del sistema (zonas, capturas, eventos, Vault)
  - **improve:** Startup log muestra estado de Vault al activarse el plugin

  ### v1.1.3
  - **fix:** Corregido error `SQLITE_BUSY` al inicializar (pool maxSize=1, WAL+busy_timeout)
  - **feat:** Soporte MySQL y MariaDB (`database.type: mysql`)
  - **fix:** BossBar migrado a Adventure API — gradientes y colores MiniMessage renderizados correctamente

  ### v1.1.0
  - Release inicial: zonas, captura, 7 consumibles PvP, Heat System, ranking

  ---

  **Autor:** zerinho23 · **Colaborador:** The_Titan19