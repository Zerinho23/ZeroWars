# ZeroWars

<div align="center">

![ZeroWars](https://img.shields.io/badge/ZeroWars-v1.1.3-red?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Paper](https://img.shields.io/badge/Paper-1.20.4+-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Build](https://img.shields.io/github/actions/workflow/status/Zerinho23/ZeroWars/build-release.yml?branch=main&style=for-the-badge&label=Build)

**Plugin competitivo de control de zonas PvP para Paper 1.20.4+**

*Domina zonas, captura minas, destruye enemigos. Sin pausas. Sin tregua.*

[Descargar v1.1.3](https://github.com/Zerinho23/ZeroWars/releases/latest) | [Issues](https://github.com/Zerinho23/ZeroWars/issues)

</div>

---

## Que es ZeroWars?

ZeroWars es un plugin **profesional, escalable y altamente optimizado** para servidores Minecraft Paper enfocado en modalidades **BoxPvP**, **FullPvP** y PvP competitivo moderno.

Los jugadores y clanes compiten por el control de zonas estrategicas. El sistema esta disenado para mantener **accion constante**, sin los tiempos muertos tipicos de survival o facciones.

---

## Caracteristicas principales

### Sistema de Zonas PvP
- Zonas rectangulares configurables (MINE, ROOM, RARE, EVENT, SPECIAL)
- Cada zona tiene nombre, nivel, dueno, estado y recompensas independientes
- Proteccion de bloques configurable por zona
- Particulas de borde visuales en tiempo real

### Sistema de Captura
- **BossBar** con progreso en tiempo real
- **ActionBar** con tiempo restante de captura
- Sistema de **contestacion**: si hay defensores, la barra se pausa
- Multiples atacantes aceleran la captura
- Cancelacion automatica si el jugador sale de la zona

### Consumibles PvP

| Consumible | Efecto | Cooldown |
|---|---|---|
| Boost de Velocidad | Speed III 10s | 30s |
| Golpe Brutal | Fuerza II 8s | 45s |
| Escudo Gravitacional | Resistencia IV 6s | 40s |
| Curacion de Emergencia | +6 corazones + Regen | 60s |
| Dash | Lanzamiento explosivo | 25s |
| Escudo de Resistencia | Resistencia max 5s | 90s |
| Sello de Vampiro | Lifesteal 8s | 75s |

### Sistema Heat / Wanted
- 3 niveles de amenaza segun capturas acumuladas
- Nivel maximo: el jugador brilla y aparece en anuncio global
- Recompensa con multiplicador por matar a un jugador con heat maximo

### Base de Datos
- **SQLite** - sin configuracion, perfecto para servidores pequenos/medianos
- **MySQL / MariaDB** - recomendado para alta concurrencia
- Pool de conexiones HikariCP en ambos modos
- Operaciones 100% asincronas

---

## Base de Datos - Configuracion

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
    username: "tu_usuario"
    password: "tu_contrasena"
    ssl: false
```

ZeroWars creara las tablas automaticamente al arrancar.

---

## Instalacion

1. Descarga el `.jar` de los [Releases](https://github.com/Zerinho23/ZeroWars/releases/latest)
2. Copialo a `plugins/`
3. Reinicia el servidor
4. Edita `plugins/ZeroWars/config.yml`
5. Usa `/zw reload` para aplicar cambios sin reiniciar

**Requisitos:** Paper 1.20.4+ | Java 21+ | PlaceholderAPI (opcional)

---

## Comandos

| Comando | Descripcion | Permiso |
|---|---|---|
| `/zerowars reload` | Recarga config | `zerowars.admin` |
| `/zone` | Info de zona actual | `zerowars.player` |
| `/zones` | Lista zonas | `zerowars.player` |
| `/top` | Ranking | `zerowars.player` |
| `/events` | Eventos activos | `zerowars.player` |

---

## Changelog

### v1.1.3
- **fix:** Corregido `SQLITE_BUSY` al inicializar el plugin
- **feat:** Soporte MySQL y MariaDB (`database.type: mysql`)
- Pool SQLite forzado a 1 conexion (evita contention de escrituras)
- PRAGMAs SQLite via `connectionInitSql` (WAL + foreign_keys + busy_timeout=5000)
- Dialectos SQL diferenciados en DAOs (`INSERT OR REPLACE` vs `ON DUPLICATE KEY UPDATE`)

### v1.1.2
- Sistema de clanes integrado
- Ranking por tiempo dominado
- API publica para integracion

### v1.1.0
- Release inicial: captura BossBar/ActionBar, 7 consumibles PvP, Heat System

---

**Autor:** zerinho23 | **Colaborador:** The_Titan19