# Multiverse-Instancia

**Plugin Minecraft (Paper/Spigot)** — Organizacion: Nyanhalla
**API requerida:** Multiverse-Core 5.6.1
**Minecraft:** 1.20.4 - 1.21.x
**Java:** 21

---

## Resumen

Plugin que permite crear instancias paralelas de mapas adventure (CTM, dungeons, mapas con checkpoints) para que multiples equipos puedan jugarlos concurrentemente en el mismo servidor.

Un equipo juega en `ctm_vortex_c1`, otro en `ctm_vortex_c2` — misma mapa, misma dificultad, cero interferencia.

---

## Concepto

### El problema

Mapas como **CTM (Complete The Map)** o dungeons adventure tienen un único mundo. Un equipo lo juega, otros esperan. O se montan multiples servidores/contenedores Docker —一架庞大事.

### La solucion

Multiverse-Instancia clona worlds de Multiverse en instancias aisladas, cada una con su propio:
- Estado de jugadores (inventario, ubicacion, progreso)
- Progreso de win conditions (monumentos CTM, checkpoints, boss HP)
- Configuracion de condiciones de victoria
- TTL / auto-reset

---

## Configuracion

### config.yml

```yaml
# --- Ajustes globales ---
instance:
  prefix: "inst_"                    # prefijo para mundos instancia
  max-instances: 10                  # maximo de instancias activas
  default-ttl: 3600                  # TTL por defecto en segundos (0 = sin limite)
  auto-cleanup-on-victory: true      # limpiar instancia al cumplir win condition
  reset-on-cleanup: true              # resetear mundo al limpiar
  clone-mode: "mirror"               # mirror = copia exacta del mundo base

# --- Win Condition: CTM ---
ctm:
  enabled: true
  monument-blocks:
    - "NETHERITE_BLOCK"
    - "BEACON"
  require-all: true                   # todos los monumentos para victoria
  victory-trigger: "broadcast"        # broadcast | reward | command
  victory-commands:
    - "bc §6¡Equipo %team% ha completado el mapa!"
    - "give %player% diamond 1"
  broadcast-title:
    title: "§6§l¡VICTORIA!"
    subtitle: "§eMapa completado"
    fade-in: 10
    stay: 60
    fade-out: 20

# --- Win Condition: Dungeon ---
dungeon:
  enabled: true
  boss-bar:
    entity-type: "WITHER_SKELETON"     # entidad que actua como boss
    display-name: "§c§lDungeon Boss"
    health-bar-sync: true
  victory-on-death: true              # victoria cuando el boss muere
  phases: []                          # fases opcionales (orda 1, 2, 3...)
  victory-trigger: "command"
  victory-commands:
    - "bc §6¡Dungeon completado por %team%!"

# --- Win Condition: Checkpoint ---
checkpoint:
  enabled: true
  max-checkpoints: 10
  save-on-advance: true               # guardar progreso automaticamente
  partial-victory: false              # victoria con todos los checkpoints o solo al final
  check-in-region:
    type: "WORLDGUARD"                # region de cada checkpoint (future: GriefDefender)
    auto-register: true               # detectar regiones automaticamente

# --- Inventarios aislados ---
inventory:
  isolate: true                      # cada instancia = inventario propio
  save-mode: "instance"               # instance | team
  hotbar-locked: false
  enderchest-isolate: true
```

### world-config.yml (por mundo base)

```yaml
# plugins/Multiverse-Instancia/worlds/ctm_vortex.yml
base-world: "ctm_vortex"
instance-settings:
  max-concurrent: 4                   # instancias maximas de este mapa
  per-team-size: 4                    #jugadores por equipo
  auto-close-empty: 300               # segundos antes de cerrar instancia vacia
win-condition:
  type: "ctm"                         # ctm | dungeon | checkpoint | custom
  config: {}
```

---

## Comandos

### Administracion

| Comando | Descripcion |
|---------|-------------|
| `/mi create <nombre_mapa> [equipo]` | Crear instancia de un mapa base |
| `/mi list` | Listar instancias activas |
| `/mi info <instancia>` | Ver detalles de una instancia |
| `/mi delete <instancia>` | Eliminar instancia (sin reset) |
| `/mi force-reset <instancia>` | Forzar reset de una instancia |
| `/mi purge` | Eliminar instancias huérfanas |
| `/mi reload` | Recargar configuracion |

### Jugadores

| Comando | Descripcion |
|---------|-------------|
| `/mi join <instancia>` | Unirse a una instancia |
| `/mi leave` | Salir de la instancia actual |
| `/mi status` | Ver progreso de win condition |
| `/mi teleport <checkpoint>` | Teleport a checkpoint (si esta habilitado) |

### Equipos

| Comando | Descripcion |
|---------|-------------|
| `/mi team create <nombre>` | Crear equipo |
| `/mi team invite <jugador>` | Invitar a equipo |
| `/mi team accept` | Aceptar invitación |
| `/mi team leave` | Dejar equipo |
| `/mi team info` | Ver estado del equipo |

### Alias

```
/mi = /multiverseinstancia = /minstancia
```

---

## Explicacion Tecnica

### Arquitectura

```
com.nyanhalla.multiverseinstancia/
├── MultiverseInstancia.java              # Plugin principal, onEnable/onDisable
├── commands/
│   ├── InstanceCommand.java              # /mi create, list, info, delete, force-reset
│   ├── JoinCommand.java                  # /mi join, leave
│   ├── StatusCommand.java                # /mi status
│   └── TeamCommand.java                  # /mi team subcommands
├── core/
│   ├── InstanceManager.java              # Lifecycle de instancias (create/delete/expire)
│   ├── WorldCloner.java                  # Wrapper sobre Multiverse-WorldManager
│   ├── Instance.java                    # Entidad: world + metadata + players + winstate
│   └── WinConditionEngine.java           # Motor de evaluacion de win conditions
├── winconditions/
│   ├── WinCondition.java                 # Interfaz abstracta
│   ├── CTMWinCondition.java              # Evalua monumentos bloqueados
│   ├── DungeonWinCondition.java          # Evalua muerte de boss + fases
│   ├── CheckpointWinCondition.java       # Evalua registro de checkpoints
│   └── CustomWinCondition.java           # Scriptable via config (future)
├── listeners/
│   ├── PlayerMoveListener.java           # Detecta entrada en regiones checkpoint
│   ├── BlockBreakListener.java           # Detecta destruccion de monumentos CTM
│   ├── EntityDeathListener.java          # Detecta muerte de boss en dungeon
│   ├── PlayerQuitListener.java           # Gestiona abandono de instancia
│   └── WorldListener.java                # Detecta unload de mundo instancia
├── storage/
│   ├── InstanceDataStore.java            # Interface
│   ├── YAMLInstanceStore.java           # Almacenamiento YAML (defecto)
│   └── SQLInstanceStore.java            # Almacenamiento SQLite/MySQL (future)
├── models/
│   ├── Team.java                        # Equipo con miembros y tag
│   ├── Checkpoint.java                  # checkpoint {id, region, location}
│   └── Monument.java                     # monumento {material, location, broken}
└── utils/
    ├── InstanceUtils.java               # Helpers de instancia
    └── MessageUtils.java                # Formateo de mensajes con color
```

### Flujo: crear una instancia

```
1. /mi create ctm_vortex EquipoRojo
         │
2. InstanceManager.createInstance(baseWorld="ctm_vortex", team="EquipoRojo")
         │
3. WorldCloner.cloneWorld("ctm_vortex", "inst_ctm_vortex_EquipoRojo_001")
         │
4. Multiverse API: mv world clone ctm_vortex inst_ctm_vortex_EquipoRojo_001
         │
5. Instance#new(team, world, ttl, winCondition).save()
         │
6. Broadcast: "Instancia creada para EquipoRojo"
         │
7. InventorySnapshot.take(instanceId)  -- guarda inventario limpio
```

### Flujo: win condition CTM

```
1. Player destroys NETHERITE_BLOCK
         │
2. BlockBreakListener detecta el evento en mundo de instancia
         │
3. instanceId = fromWorld(event.getWorld())
         │
4. CTMWinCondition.onBlockBreak(instanceId, block)
         │
5. markMonumentBroken(instanceId, block)
         │
6. if (allMonumentsBroken) → triggerVictory()
         │                            │
         │                     broadcast / give reward
         │                     if auto-cleanup → deleteInstance()
```

### Flujo: checkpoint

```
1. Player entra en region WORLDGUARD "ctm_vortex_cp3"
         │
2. PlayerMoveListener detecta movimiento
         │
3. CheckpointWinCondition.verifyCheckpoint(instanceId, player, region)
         │
4. si es nuevo checkpoint → saveProgress() + notifyTeam()
         │
5. if (lastCheckpoint) → partial_victory ? notify : triggerVictory()
```

### Integracion con Multiverse-Core 5.6.1

El plugin extiende la API de Multiverse-Core:

```java
// Depend: soft-depend Multiverse-Core
// Para clonar worlds:
mv world clone <original> <clone> [generator]

// Para listar worlds:
mv list

// Navegacion de API relevante en MV 5.6.1:
WorldManager#getWorld(String worldName)
CoreWorld#getCBWorld()
WorldTracker#startTracking(World)
```

**Nota:** El plugin NO modifica Multiverse-Core. Lo utiliza via API publica.
Si Multiverse no esta presente, el plugin se deshabilita con mensaje claro.

### Almacenamiento

**YAML (defecto)** — `plugins/Multiverse-Instancia/instances/`

```
instances/
├── ctm_vortex_EquipoRojo_001.yml   # metadata, winstate, players
├── ctm_vortex_EquipoAzul_002.yml
└── index.yml                        # registro global de instancias
```

**Esquema Instance YAML:**

```yaml
id: "ctm_vortex_EquipoRojo_001"
base-world: "ctm_vortex"
display-name: "§cEquipoRojo §7- CTM Vortex"
team: "EquipoRojo"
world: "inst_ctm_vortex_EquipoRojo_001"
created-at: 1714000000
expires-at: 1714003600
status: "active"                    # active | completed | expired
win-condition:
  type: "ctm"
  monuments:
    - location: "world:100,64,-200"
      broken: true
    - location: "world:200,64,-300"
      broken: false
players:
  - "Jugador1"
  - "Jugador2"
```

### Permisos

| Permiso | Descripcion | Default |
|---------|-------------|---------|
| `mi.admin.create` | Crear instancias | op |
| `mi.admin.delete` | Eliminar instancias | op |
| `mi.admin.list` | Listar todas las instancias | op |
| `mi.admin.others` | Ver/gestionar instancias de otros | op |
| `mi.player.join` | Unirse a instancia | true |
| `mi.player.create` | Crear instancia propia | true |
| `mi.player.team` | Usar sistema de equipos | true |
| `mi.bypass.limit` | Ignorar limite de instancias | false |

---

## Dependencias

```xml
<dependencies>
    <!-- Paper API -->
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.20.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <!-- Multiverse-Core 5.6.1 -->
    <dependency>
        <groupId>com.onarandombox</groupId>
        <artifactId>Multiverse-Core</artifactId>
        <version>5.6.1</version>
        <scope>provided</scope>
    </dependency>
    <!-- WorldGuard (opcional, para checkpoints) -->
    <dependency>
        <groupId>com.sk89q.worldguard</groupId>
        <artifactId>worldguard-bukkit</artifactId>
        <version>7.0.9</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**soft-depend:** Multiverse-Core
**depend:** Ninguna (funciona sin WorldGuard si checkpoints deshabilitados)

---

## Plan de implementacion

### Fase 1: Core (~2h)
- [ ] Estructura del proyecto + pom.xml
- [ ] Plugin principal con lifecycle
- [ ] InstanceManager basico (create/delete/list)
- [ ] WorldCloner usando MV API
- [ ] Almacenamiento YAML
- [ ] Comandos /mi basicos

### Fase 2: Win Conditions (~1.5h)
- [ ] WinConditionEngine + interfaz
- [ ] CTMWinCondition (block break detection)
- [ ] CheckpointWinCondition (region detection)
- [ ] DungeonWinCondition (boss death)
- [ ] Victory trigger system

### Fase 3: Equipos y Jugadores (~1h)
- [ ] TeamManager
- [ ] Inventory snapshots
- [ ] PlayerQuitListener (gestionar abandono)
- [ ] Comandos de equipo

### Fase 4: polish (~0.5h)
- [ ] TTL + auto-cleanup
- [ ] MensajesBroadcast
- [ ] Permisos
- [ ] Documentacion plugin.yml

---

## Compilacion

```bash
mvn clean package
# Output: target/MultiverseInstancia-1.0.0.jar
```

## Instalacion

1. Instalar Multiverse-Core 5.6.1 en el servidor
2. Copiar JAR a `plugins/`
3. Reiniciar servidor
4. Configurar `plugins/Multiverse-Instancia/config.yml`
5. Usar `/mi create <mapa> <equipo>` para crear instancias

---

## Roadmap

- [ ] Soporte SQL (SQLite/MySQL) para persistencia
- [ ] Integracion con GriefDefender para checkpoints
- [ ] GUI de gestion de instancias (menu interactivo)
- [ ] Webhook de victoria (Discord)
- [ ] Migracion de inventarios entre instancias
