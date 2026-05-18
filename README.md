# ⚔ ZeroWars

<div align="center">

![ZeroWars Banner](https://img.shields.io/badge/ZeroWars-v1.0.0-red?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Paper](https://img.shields.io/badge/Paper-1.20.4+-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**Plugin competitivo de control de zonas PvP para Paper 1.20.4+**

*Domina zonas, captura minas, destruye enemigos. Sin pausas. Sin tregua.*

</div>

---

## 🎯 ¿Qué es ZeroWars?

ZeroWars es un plugin **profesional, escalable y altamente optimizado** para servidores Minecraft Paper enfocado en modalidades **BoxPvP**, **FullPvP** y PvP competitivo moderno.

Los jugadores y clanes compiten por el control de zonas estratégicas: minas, rooms, áreas raras y arenas de evento. El sistema está diseñado para mantener **acción constante**, sin los tiempos muertos típicos de survival o facciones.

---

## ✨ Características principales

### ⚔ Sistema de Zonas PvP
- Zonas rectangulares configurables (MINE, ROOM, RARE, EVENT, SPECIAL)
- Cada zona tiene nombre, nivel, dueño, estado y recompensas independientes
- Protección de bloques configurable por zona
- Partículas de borde visuales en tiempo real

### 🏴 Sistema de Captura
- **BossBar** con progreso en tiempo real
- **ActionBar** con tiempo restante de captura
- Sistema de **contestación**: si hay defensores, la barra se pausa
- Múltiples atacantes aceleran la captura
- Cooldown de recaptura configurable por tipo de zona
- Cancelación automática si el jugador sale de la zona

### ⚡ Consumibles PvP (sin comandos)
Todos los poderes funcionan mediante **ítems físicos** con click derecho:

| Consumible | Efecto | Cooldown |
|---|---|---|
| ⚡ Boost de Velocidad | Speed III por 10s | 30s |
| 💪 Golpe Brutal | Fuerza II por 8s | 45s |
| 🛡 Escudo Gravitacional | Resistencia IV por 6s | 40s |
| ❤ Curación de Emergencia | +6 corazones + Regen | 60s |
| 🌀 Dash | Lanzamiento explosivo | 25s |
| 🔮 Escudo de Resistencia | Resistencia máx por 5s | 90s |
| 🩸 Sello de Vampiro | Lifesteal por 8s | 75s |

### 🔥 Sistema Heat / Wanted
- 3 niveles de amenaza según capturas acumuladas
- Nivel máximo: el jugador **brilla** y aparece en anuncio global
- Recompensa por matar a un jugador con heat máximo (multiplicador configurable)
- El heat expira con el tiempo o al morir

### 🏆 Ranking y Estadísticas
- Top por kills, capturas y tiempo dominado
- Datos persistentes en SQLite
- Integración completa con **PlaceholderAPI**

### 🌩 Eventos Automáticos
- **Doble Loot** — x2 recompensas globales
- **Mina Legendaria** — zona épica temporal
- **Zona de Oro** — x5 en zona específica
- **Lluvia de Recompensas** — drops extra a dueños de zonas
- **Guerra Total** — x4 recompensas + PvP sin restricciones
- Scheduler automático configurable

### 👥 Sistema de Clanes
- Crear, disolver e invitar a clanes internamente
- Miembros del mismo clan comparten zonas y no pueden atacarse
- Compatible con SimpleClans (configurable)

---

## 📦 Instalación

### Requisitos
- Paper 1.20.4 o superior
- Java 21+
- (Opcional) PlaceholderAPI

### Pasos
1. Descarga `ZeroWars-1.0.0.jar` de [Releases](../../releases)
2. Cópialo a la carpeta `plugins/` de tu servidor
3. Reinicia el servidor
4. Edita los archivos de configuración en `plugins/ZeroWars/`
5. Usa `/zw reload` para aplicar cambios sin reiniciar

---

## ⚙ Configuración

| Archivo | Descripción |
|---|---|
| `config.yml` | Configuración general, base de datos, captura, heat, ranking |
| `messages.yml` | Todos los mensajes (MiniMessage + HEX colors) |
| `zones.yml` | Definición y coordenadas de zonas |
| `rewards.yml` | Recompensas por captura, pasivas y por nivel |
| `consumables.yml` | Items consumibles PvP con cooldowns y efectos |
| `cooldowns.yml` | Cooldown global y por consumible |
| `events.yml` | Eventos automáticos con scheduler |

---

## 💻 Comandos

### Administración (`/zerowars` o `/zw`)

| Comando | Permiso | Descripción |
|---|---|---|
| `/zw reload` | `zerowars.admin.reload` | Recarga toda la configuración |
| `/zw createzone <id> [tipo]` | `zerowars.admin.createzone` | Crea una zona en tu posición |
| `/zw deletezone <id>` | `zerowars.admin.deletezone` | Elimina una zona |
| `/zw giveconsumable <player> <id> [cantidad]` | `zerowars.admin.giveconsumable` | Da consumibles |
| `/zw startevent <id>` | `zerowars.admin.startevent` | Inicia un evento |
| `/zw stopevent <id>` | `zerowars.admin.stopevent` | Detiene un evento |
| `/zw debug` | `zerowars.admin.debug` | Info de debug en tiempo real |

### Jugadores

| Comando | Permiso | Descripción |
|---|---|---|
| `/zone` | `zerowars.player` | Info de la zona actual |
| `/zones` | `zerowars.player` | Lista todas las zonas |
| `/top [kills\|captures\|time]` | `zerowars.player` | Ranking del servidor |
| `/events` | `zerowars.player` | Eventos activos |

---

## 🔑 Permisos

| Permiso | Descripción | Default |
|---|---|---|
| `zerowars.admin` | Acceso admin completo | OP |
| `zerowars.moderator` | Acceso moderador | false |
| `zerowars.player` | Acceso base jugador | true |
| `zerowars.bypass` | Salta todas las restricciones | OP |
| `zerowars.consumables.bypass.cooldown` | Sin cooldown en consumibles | OP |
| `zerowars.bypass.pvp` | Sin restricciones PvP en zonas | OP |

---

## 📊 PlaceholderAPI

```
%zerowars_zone%              → Nombre de zona actual
%zerowars_zone_owner%        → Dueño de la zona
%zerowars_zone_capture%      → Porcentaje de captura
%zerowars_zone_level%        → Nivel de la zona
%zerowars_zone_state%        → Estado (NEUTRAL/OWNED/CAPTURING/CONTESTED)
%zerowars_heat%              → Nivel de heat del jugador
%zerowars_heat_name%         → Nombre del nivel de heat
%zerowars_kills%             → Kills totales
%zerowars_deaths%            → Deaths totales
%zerowars_captures%          → Capturas totales
%zerowars_domine_time%       → Tiempo dominado (formateado)
%zerowars_kd%                → Ratio K/D
%zerowars_clan%              → Nombre del clan
%zerowars_cooldown_<id>%     → Cooldown restante de consumible
%zerowars_top_kills_1%       → Jugador #1 en kills
%zerowars_top_captures_1%    → Jugador #1 en capturas
%zerowars_events%            → Número de eventos activos
```

---

## 🛠 Tecnologías

- **Java 21** + Paper API 1.20.4+
- **Gradle** + Shadow JAR (dependencias shadeadas)
- **SQLite** + HikariCP (connection pooling)
- **Adventure / MiniMessage** (soporte HEX completo)
- **PlaceholderAPI** (integración opcional)
- Arquitectura modular: managers, DAOs, listeners, API pública

---

## 🔌 API para Desarrolladores

```java
ZeroWarsAPI api = ZeroWars.getInstance().getAPI();

// Zona del jugador
Optional<Zone> zone = api.getPlayerZone(player);

// ¿Está el jugador en una zona?
boolean inZone = api.isInZone(player);

// Iniciar un evento
api.startEvent("double_loot");

// Multiplicador activo para una zona
double multiplier = api.getRewardMultiplier("mina_roja");

// Dar un consumible como ItemStack
ItemStack item = api.buildConsumableItem("speed_boost", 1);
```

---

## 📝 Compilar desde código fuente

```bash
git clone https://github.com/zerinho23/ZeroWars.git
cd ZeroWars
gradle shadowJar
# Resultado: build/libs/ZeroWars-1.0.0.jar
```

---

## 👥 Autores

| Rol | Usuario |
|---|---|
| 🧑‍💻 Autor principal | **zerinho23** |
| 🤝 Colaborador | **The_Titan19** |

---

## 📄 Licencia

MIT License — libre de usar, modificar y distribuir con atribución.

---

<div align="center">

*ZeroWars — Hecho con ❤ para servidores PvP competitivos*

</div>
