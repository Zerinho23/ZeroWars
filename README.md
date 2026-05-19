# ZeroWars

  <div align="center">

  ![ZeroWars](https://img.shields.io/badge/ZeroWars-v1.4.0-red?style=for-the-badge&logo=minecraft)
  ![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk)
  ![Paper](https://img.shields.io/badge/Paper-1.20.4+-blue?style=for-the-badge)
  ![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
  ![Build](https://img.shields.io/github/actions/workflow/status/Zerinho23/ZeroWars/build-release.yml?branch=main&style=for-the-badge&label=Build)

  **Plugin competitivo de control de zonas PvP para Paper 1.20.4+**

  *Domina zonas, captura minas, destruye enemigos. Sin pausas. Sin tregua.*

  [**Descargar v1.4.0**](https://github.com/Zerinho23/ZeroWars/releases/latest) · [Issues](https://github.com/Zerinho23/ZeroWars/issues) · [Releases](https://github.com/Zerinho23/ZeroWars/releases)

  </div>

  ---

  ## ¿Qué es ZeroWars?

  ZeroWars es un plugin de control de zonas PvP para servidores **Paper 1.20.4+**. Los jugadores o clanes capturan zonas para ganar recompensas económicas, items y ventajas progresivas. El servidor admin gestiona todo desde un menú interactivo sin editar YAMLs.

  ---

  ## Características principales

  | Categoría | Función |
  |-----------|---------|
  | ⚔️ **Zonas PvP** | Captura y defensa de zonas con sistema de progresión por nivel |
  | 🪓 **Wand Tool** | Hacha dorada para marcar pos1/pos2 y crear zonas visualmente |
  | 🎮 **Admin GUI** | Menú interactivo completo (/zw menu) sin tocar archivos YAML |
  | 💰 **Recompensas** | Dinero (Vault), items y comandos al capturar, pasivas y por nivel |
  | ✏️ **Editor de Rewards** | Edita montos, intervalos y multiplicadores desde el GUI en tiempo real |
  | 📅 **Eventos** | Zonas de evento programadas con scheduler configurable |
  | 🧪 **Consumibles** | Items especiales con efectos temporales (speed boost, etc.) |
  | 🏆 **Ranking** | Top jugadores por capturas y dominación |
  | 👥 **Clanes** | Sistema de clanes con estadísticas por grupo |
  | 🗃️ **Base de datos** | SQLite (default) o MySQL, con HikariCP y migración automática |
  | 📊 **PlaceholderAPI** | Variables para ScoreBoards, TAB, chat |
  | 🔌 **Vault** | Economía opcional — funciona sin Vault si no hay rewards de dinero |

  ---

  ## Instalación

  ### Requisitos
  - Paper **1.20.4** o superior (Purpur compatible)
  - Java **17+**
  - (Opcional) Vault + plugin de economía para rewards de dinero
  - (Opcional) PlaceholderAPI para variables en ScoreBoard/TAB

  ### Pasos
  1. Descarga **[ZeroWars-1.4.0.jar](https://github.com/Zerinho23/ZeroWars/releases/latest)**
  2. Colócalo en la carpeta `plugins/` del servidor
  3. Reinicia el servidor
  4. Configura `plugins/ZeroWars/config.yml` (base de datos, general)
  5. Crea tus zonas con `/zw menu` → **Crear Nueva Zona** (sin editar YAML)
  6. Configura rewards en `rewards.yml` o desde el propio menú

  ---

  ## Comandos

  ### Jugadores
  | Comando | Descripción | Permiso |
  |---------|-------------|---------|
  | `/zones` | Lista todas las zonas activas | `zerowars.zones` |
  | `/zone <id>` | Info detallada de una zona | `zerowars.zone` |
  | `/top` | Ranking de jugadores | `zerowars.top` |
  | `/events` | Eventos activos | `zerowars.events` |

  ### Administración
  | Comando | Descripción | Permiso |
  |---------|-------------|---------|
  | `/zw menu` | Abre el menú admin principal | `zerowars.admin` |
  | `/zw createzone <id> <nombre>` | Crea zona vía comando (alternativo al wand) | `zerowars.admin` |
  | `/zw deletezone <id>` | Elimina una zona | `zerowars.admin` |
  | `/zw setreward <zona> <reward_id>` | Asigna reward a una zona | `zerowars.admin` |
  | `/zw giveconsumable <player> <id> <cantidad>` | Da consumible a un jugador | `zerowars.admin` |
  | `/zw reload` | Recarga toda la configuración | `zerowars.admin` |
  | `/zw debug` | Toggle de logs de depuración | `zerowars.admin` |
  | `/zw version` | Muestra la versión instalada | `zerowars.admin` |

  ---

  ## Menú Admin GUI (/zw menu)

  El menú principal de administración agrupa todas las funciones sin necesidad de editar archivos:

  ```
  ┌─────────────────────────────────────────────────────┐
  │  ⚔ ZeroWars Admin  v1.4.0                          │
  ├──────┬──────┬──────┬──────────────────────────────  │
  │Zonas │Event.│Recarg│   Estado Plugin                │
  ├──────┼──────┼──────┼──────────────────────────────  │
  │Crear │Recom.│Vault │   Debug                        │
  │ Zona │      │      │                                │
  ├──────┴──────┴──────┴──────────────────────────────  │
  │                    [Cerrar]                          │
  └─────────────────────────────────────────────────────┘
  ```

  ### Crear Nueva Zona — Wand Tool

  1. Click en **"⚒ Crear Nueva Zona"** → recibes un hacha dorada especial
  2. **Clic izquierdo** en un bloque → marca **Pos1** (esquina mínima)
  3. **Clic derecho** en un bloque → marca **Pos2** (esquina máxima)
  4. Con ambas posiciones marcadas → se abre el **Menú de Creación** automáticamente
  5. En el menú seleccionas:
     - **Tipo** de zona: MINE / ROOM / RARE / EVENT / SPECIAL
     - **Nombre** → click en el botón → escribe en el chat (soporta `&e`, `&#RRGGBB`)
  6. Click en **"✔ Crear Zona"** → se guarda en `zones.yml` + base de datos y el hacha desaparece

  ### Gestión de Recompensas

  1. Click en **"Recompensas"** → lista de todos los rewards configurados
  2. Click en un reward → detalle con montos actuales
  3. Editar directamente desde el GUI:
     - **Clic izq en "Al Capturar"** → escribe el nuevo monto en el chat
     - **Clic izq en "Pasiva"** → edita el dinero pasivo
     - **Clic der en "Pasiva"** → edita el intervalo en segundos
     - **Clic en "Multiplicador"** → edita el multiplicador por nivel
     - Los cambios se guardan en `rewards.yml` inmediatamente

  ### Gestión de Zonas

  - Lista paginada de todas las zonas con estado (activa/inactiva, nivel, capturas)
  - Click en una zona → detalle completo
    - Activar / desactivar zona
    - Ver rewards asignados
    - Ver estadísticas de captura

  ### Gestión de Eventos

  - Lista de todos los eventos y su estado
  - Iniciar o finalizar eventos manualmente
  - Ver conteo de eventos activos

  ---

  ## Configuración

  ### config.yml

  ```yaml
  general:
    debug: false
    language: "es"
    prefix: "<gradient:#ff4444:#ff8800><bold>ZeroWars</bold></gradient>"

  database:
    type: SQLITE        # SQLITE o MYSQL
    mysql:
      host: localhost
      port: 3306
      database: zerowars
      username: root
      password: ""
      pool-size: 10

  capture:
    required-players: 1        # Jugadores mínimos para capturar
    capture-time-seconds: 30   # Tiempo de captura base
    passive-tick-seconds: 60   # Tick de rewards pasivos

  zones:
    max-level: 5               # Nivel máximo de zona
    points-per-level: 30       # Puntos para subir de nivel
  ```

  ### zones.yml

  Generado automáticamente al crear zonas con el wand. No necesitas editarlo manualmente.

  ```yaml
  zones:
    mina_roja:
      name: "&cMina Roja"
      type: MINE
      world: world
      minX: 100.0
      minY: 60.0
      minZ: 100.0
      maxX: 120.0
      maxY: 80.0
      maxZ: 120.0
      level: 1
      maxLevel: 5
      pointsPerLevel: 30
      enabled: true
      rewardIds:
        - mina_roja_reward
  ```

  ### rewards.yml

  ```yaml
  rewards:
    mina_roja_reward:
      on-capture:
        money: 500
        commands:
          - "zerowars giveconsumable %player% speed_boost 1"
        items:
          - material: DIAMOND
            amount: 3
            name: "<aqua>Diamante de la Mina"
            lore:
              - "<gray>Obtenido en la Mina Roja"

      passive:
        interval: 60       # segundos entre cada reward pasivo
        money: 100
        items:
          - material: IRON_INGOT
            amount: 5

      per-level:
        money-multiplier: 1.5
        extra-items-per-level:
          1:
            - material: GOLD_INGOT
              amount: 2
          3:
            - material: DIAMOND
              amount: 1
          5:
            - material: NETHERITE_SCRAP
              amount: 1
  ```

  ### consumables.yml

  ```yaml
  consumables:
    speed_boost:
      name: "<yellow><bold>Speed Boost"
      material: SUGAR
      lore:
        - "<gray>Velocidad II por 30 segundos"
      effects:
        - type: SPEED
          amplifier: 1
          duration: 600   # ticks (20 ticks = 1s)
      cooldown: 60        # segundos entre usos
  ```

  ---

  ## Tipos de Zona

  | Tipo | Descripción |
  |------|-------------|
  | `MINE` | Zona de minería — alta frecuencia de capturas |
  | `ROOM` | Sala central — alta disputa PvP |
  | `RARE` | Zona rara — alta recompensa, difícil de mantener |
  | `EVENT` | Solo activa durante eventos programados |
  | `SPECIAL` | Zona especial configurada por el servidor |

  ---

  ## PlaceholderAPI

  | Variable | Descripción |
  |----------|-------------|
  | `%zerowars_captures%` | Capturas totales del jugador |
  | `%zerowars_dominaciones%` | Tiempo de dominación acumulado |
  | `%zerowars_clan%` | Clan del jugador |
  | `%zerowars_rank%` | Posición en el ranking |
  | `%zerowars_zone_owner_<id>%` | Dueño actual de la zona |
  | `%zerowars_zone_level_<id>%` | Nivel actual de la zona |

  ---

  ## Base de datos

  ZeroWars usa SQLite por defecto (sin configuración extra) o MySQL para servidores en red.

  - **SQLite**: archivo `plugins/ZeroWars/database.db` — ideal para un solo servidor
  - **MySQL**: configura el bloque `database.mysql` en `config.yml` y cambia `type: MYSQL`
  - Las tablas se crean automáticamente al primer arranque
  - La migración entre versiones es automática

  ---

  ## Permisos

  | Permiso | Descripción | Default |
  |---------|-------------|---------|
  | `zerowars.admin` | Acceso total a comandos y GUI de admin | OP |
  | `zerowars.zones` | Ver lista de zonas | true |
  | `zerowars.zone` | Ver detalle de zona | true |
  | `zerowars.top` | Ver ranking | true |
  | `zerowars.events` | Ver eventos | true |
  | `zerowars.bypass` | Ignora protección de zonas | OP |

  ---

  ## Changelog

  ### v1.4.0
  - **NUEVO**: Hacha wand para crear zonas — marca pos1/pos2 en el mapa sin editar YAML
  - **NUEVO**: `ZoneCreateGui` — menú visual para configurar tipo, nombre y área antes de crear
  - **NUEVO**: `ChatInputManager` — sistema genérico para pedir texto en chat desde GUIs
  - **NUEVO**: `RewardListGui` — lista todos los rewards desde el menú admin
  - **NUEVO**: `RewardDetailGui` — edita dinero, intervalo y multiplicador de rewards en tiempo real
  - **MEJORADO**: `MainMenuGui` — nuevos botones "Crear Zona" y "Recompensas"

  ### v1.3.0
  - **NUEVO**: Sistema GUI admin completo — `MainMenuGui`, `ZoneListGui`, `ZoneDetailGui`, `EventListGui`
  - **NUEVO**: `GuiManager` y `GuiListener` — apertura de GUIs desde `/zw menu`
  - **NUEVO**: Zonas guardadas en `zones.yml` con `/zw createzone`
  - **NUEVO**: Soporte de color codes legacy (`&5`, `&#RRGGBB`) en `MessageUtil`
  - **FIX**: Error de compilación en `buildItem()` de `ZeroWarsCommand`

  ### v1.2.0
  - **NUEVO**: `RewardManager` — rewards al capturar, pasivos y por subida de nivel
  - **NUEVO**: Integración con Vault para rewards de economía
  - **NUEVO**: Tick pasivo de rewards en `ZoneManager`
  - **NUEVO**: Comando `/zw setreward <zona> <reward_id>`
  - **NUEVO**: `rewards.yml` con soporte de dinero, items y comandos

  ### v1.1.3
  - **FIX**: Error `SQLITE_BUSY` en escrituras concurrentes (HikariCP + WAL mode)
  - **NUEVO**: Soporte MySQL completo con pool de conexiones
  - **FIX**: BossBar con Adventure API (compatible Paper 1.20.4+)

  ---

  ## Compilar desde fuente

  ```bash
  git clone https://github.com/Zerinho23/ZeroWars.git
  cd ZeroWars
  ./gradlew shadowJar
  # Output: build/libs/ZeroWars-1.4.0.jar
  ```

  **Requisitos para compilar:**
  - JDK 17+
  - Gradle (wrapper incluido)

  ---

  ## Licencia

  MIT License — ver [LICENSE](LICENSE) para detalles.

  ---

  <div align="center">

  Desarrollado por **zerinho23** & **The_Titan19**

  [Descargar última versión](https://github.com/Zerinho23/ZeroWars/releases/latest)

  </div>