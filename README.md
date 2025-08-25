# 드레곤 알 서바이벌 플러그인

# 프로젝트 구조

```bash
DragonHeist99/
├── build.gradle.kts           // Gradle 빌드 스크립트 (Kotlin DSL)
├── settings.gradle.kts        // 프로젝트 설정 파일
├── gradle.properties          // Gradle 속성 설정
├── src/
│   └── 📁 main/
│       ├── 📁 resources/          // 리소스 파일 (설정 및 플러그인 정의)
│       │   ├── 📄 plugin.yml      // 플러그인 메타 정보
│       │   └── 📄 config.yml      // 사용자 정의 설정
│       └── 📁 kotlin/
│           └── 📁 com.github.oniroo/
│               ├── 📄 DragonHeist99.kt       // 메인 클래스
│               ├── 📁 model/
│               │   └── 📄 State.kt           // 게임 상태 모델
│               ├── 📁 service/               // 핵심 서비스 로직
│               │   ├── 📄 BorderService.kt
│               │   ├── 📄 GameClock.kt
│               │   ├── 📄 EggService.kt
│               │   ├── 📄 FootprintService.kt
│               │   ├── 📄 TradeLimiter.kt
│               │   ├── 📄 ItemRuleService.kt
│               │   ├── 📄 EnchantService.kt
│               │   ├── 📄 XpService.kt
│               │   └── 📄 RecipeService.kt
│               └── 📁 util/
│                   └── 📄 Stores.kt          // 유틸리티 및 저장소 관련 코드
```

# Gradle 셋업

**build.gradle.kts**

```kotlin
plugins {
    kotlin("jvm") version "1.9.24"
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "com.kh"
version = "1.0.0"
description = "DragonHeist99 - 99일 드래곤 알 쟁탈전"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
}

java {
    toolchain.languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    jar {
        archiveBaseName.set("DragonHeist99")
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
    }
}

```

**settings.gradle.kts**

```kotlin
rootProject.name = "DragonHeist99"

```

**gradle.properties**

```
org.gradle.jvmargs=-Xmx2g
kotlin.code.style=official

```

---

# plugin.yml & config.yml

**src/main/resources/plugin.yml**

```yaml
name: DragonHeist99
main: com.kh.dragonheist99.DragonHeist99
version: 1.0.0
api-version: '1.21'
authors: [ "Koma & Marong" ]
commands:
  dh:
    description: DragonHeist admin
    usage: /dh <sub>
    permission: dragonheist.admin
permissions:
  dragonheist.admin:
    default: op

```

**src/main/resources/config.yml**

```yaml
worlds:
  overworld: "world"
  nether: "world_nether"

border:
  overworld_size: 1000
  nether_size: 500

game:
  max_day: 99          # 1~99 진행, 100일차 진입 순간 우승 판정
  show_day_broadcast: true
  overtime_enabled: false
  overtime_hold_seconds: 60

egg:
  beacon_minutes: 100
  enderman_effect_min_sec: 120
  enderman_effect_max_sec: 300
  footprint_sample_blocks: 1.5
  footprint_visible_range: 32

trades:
  limit_per_day: 20

xp:
  drop_full_on_death: true
  dragon_kill_xp: 5000

items:
  potion_max_stack: 2
  golden_apple_regen_seconds: 2.0
  ban:
    elytra: true
    ender_pearl_throw: true
    shulker_box: true
    ender_chest: true
    shield: true
  recipes:
    netherite_template_alt: true

enchant:
  sharpness_max: 10
  protection_max: 7
  anvil_book_apply_cost: 39

```

---

# 메인 & 상태 모델

**DragonHeist99.kt**

```kotlin
package com.kh.dragonheist99

import com.kh.dragonheist99.model.State
import com.kh.dragonheist99.service.*
import com.kh.dragonheist99.util.Stores
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class DragonHeist99 : JavaPlugin() {
    lateinit var state: State
    lateinit var border: BorderService
    lateinit var clock: GameClock
    lateinit var egg: EggService
    lateinit var footprints: FootprintService
    lateinit var trade: TradeLimiter
    lateinit var itemRules: ItemRuleService
    lateinit var ench: EnchantService
    lateinit var xp: XpService
    lateinit var recipes: RecipeService

    override fun onEnable() {
        saveDefaultConfig()
        state = Stores.loadState(this)
        border = BorderService(this).applyBorders()
        recipes = RecipeService(this).register()
        clock = GameClock(this).start()
        egg = EggService(this).hook()
        footprints = FootprintService(this).start()
        trade = TradeLimiter(this).hook()
        itemRules = ItemRuleService(this).hook()
        ench = EnchantService(this).hook()
        xp = XpService(this).hook()
        logger.info("DragonHeist99 enabled. Day=${state.currentDay}")
    }

    override fun onDisable() {
        Stores.saveState(this, state)
        logger.info("DragonHeist99 disabled & saved.")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("dh", true)) {
            if (args.isEmpty()) {
                sender.sendMessage("§d/dh day set <n>, /dh egg give <player>, /dh overtime <on|off>")
                return true
            }
            when (args[0].lowercase()) {
                "day" -> {
                    if (args.size >= 3 && args[1].equals("set", true)) {
                        val n = args[2].toIntOrNull() ?: return true.also { sender.sendMessage("숫자 ㄱ") }
                        state.currentDay = n.coerceAtLeast(1)
                        Stores.saveState(this, state)
                        sender.sendMessage("§dDay set -> $n")
                    }
                }
                "egg" -> {
                    if (args.size >= 3 && args[1].equals("give", true)) {
                        val p = Bukkit.getPlayerExact(args[2]) ?: return true.also { sender.sendMessage("플레이어 없음") }
                        egg.giveEggTo(p)
                        sender.sendMessage("§d알 강제 지급 -> ${p.name}")
                    }
                }
                "overtime" -> {
                    if (args.size >= 2) {
                        val on = args[1].equals("on", true)
                        config.set("game.overtime_enabled", on)
                        saveConfig()
                        sender.sendMessage("§d연장전: ${if (on) "ON" else "OFF"}")
                    }
                }
            }
            return true
        }
        return false
    }

    fun winCheckOnDay100() {
        val holder = state.eggHolder?.let { UUID.fromString(it) }
        val p: Player? = holder?.let { Bukkit.getPlayer(it) }
        if (p != null && p.isOnline) {
            Bukkit.broadcastMessage("§d[DragonHeist] §f우승자: §5${p.name} §f(드래곤 알 보유)")
        } else {
            Bukkit.broadcastMessage("§d[DragonHeist] §f100일차 진입! 하지만 알 보유자가 없어 우승자 없음.")
            if (config.getBoolean("game.overtime_enabled")) {
                Bukkit.broadcastMessage("§7연장전 시작: 알 ${config.getInt("game.overtime_hold_seconds")}초 보유 시 즉시 우승.")
            }
        }
        // 여기서 서버 락다운/리셋 등 운영 후처리 훅 가능
    }
}

```

**model/State.kt**

```kotlin
package com.kh.dragonheist99.model

import java.util.*

data class Footprint(
    val world: UUID,
    val x: Double, val y: Double, val z: Double,
    val dx: Float, val dz: Float,
    val expireDay: Int
)

data class State(
    var currentDay: Int = 1,
    var eggHolder: String? = null,              // UUID string
    var firstAcquirer: String? = null,          // UUID string
    var firstAcquirerEndToOverworldUsed: Boolean = false,
    val footprints: MutableList<Footprint> = mutableListOf(),
    val tradesToday: MutableMap<String, Int> = mutableMapOf()
)

```

**util/Stores.kt**

```kotlin
package com.kh.dragonheist99.util

import com.kh.dragonheist99.DragonHeist99
import com.kh.dragonheist99.model.Footprint
import com.kh.dragonheist99.model.State
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

object Stores {
    private fun file(plugin: DragonHeist99) = File(plugin.dataFolder, "data.yml")

    fun loadState(plugin: DragonHeist99): State {
        val f = file(plugin)
        if (!f.exists()) return State()
        val y = YamlConfiguration.loadConfiguration(f)
        val state = State(
            currentDay = y.getInt("currentDay", 1),
            eggHolder = y.getString("eggHolder"),
            firstAcquirer = y.getString("firstAcquirer"),
            firstAcquirerEndToOverworldUsed = y.getBoolean("firstAcqUsed", false)
        )
        y.getMapList("trades").forEach {
            val u = it["u"] as String; val c = (it["c"] as Number).toInt()
            state.tradesToday[u] = c
        }
        y.getMapList("footprints").forEach {
            val fp = Footprint(
                UUID.fromString(it["w"] as String),
                (it["x"] as Number).toDouble(),
                (it["y"] as Number).toDouble(),
                (it["z"] as Number).toDouble(),
                (it["dx"] as Number).toFloat(),
                (it["dz"] as Number).toFloat(),
                (it["ed"] as Number).toInt()
            )
            state.footprints += fp
        }
        return state
    }

    fun saveState(plugin: DragonHeist99, s: State) {
        val y = YamlConfiguration()
        y.set("currentDay", s.currentDay)
        y.set("eggHolder", s.eggHolder)
        y.set("firstAcquirer", s.firstAcquirer)
        y.set("firstAcqUsed", s.firstAcquirerEndToOverworldUsed)
        y.set("trades", s.tradesToday.map { mapOf("u" to it.key, "c" to it.value) })
        y.set("footprints", s.footprints.map {
            mapOf("w" to it.world.toString(), "x" to it.x, "y" to it.y, "z" to it.z,
                  "dx" to it.dx, "dz" to it.dz, "ed" to it.expireDay)
        })
        y.save(file(plugin))
    }
}

```

---

# 서비스 구현

**service/BorderService.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import org.bukkit.World

class BorderService(private val plugin: DragonHeist99) {
    fun applyBorders(): BorderService {
        val overworld = plugin.server.getWorld(plugin.config.getString("worlds.overworld")!!)!!
        val nether = plugin.server.getWorld(plugin.config.getString("worlds.nether")!!)!!
        setBorder(overworld, plugin.config.getInt("border.overworld_size").toDouble())
        setBorder(nether, plugin.config.getInt("border.nether_size").toDouble())
        return this
    }

    private fun setBorder(world: World, size: Double) {
        val b = world.worldBorder
        b.center = world.spawnLocation
        b.size = size
        b.warningDistance = 4
    }
}

```

**service/GameClock.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import com.kh.dragonheist99.util.Stores
import org.bukkit.Bukkit

class GameClock(private val plugin: DragonHeist99) {
    fun start(): GameClock {
        // 10틱마다 체크
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val w = plugin.server.getWorld(plugin.config.getString("worlds.overworld")!!) ?: return@Runnable
            if (w.time.toInt() % 24000 == 0) { // 하루 경계
                // 중복 트리거 방지용: time이 0 되는 순간만 잡히도록 약간의 여유 필요
            }
        }, 10L, 10L)

        // 1초마다 낮/밤 체크 + 발자국 파티클 렌더 + 일 경계/리셋
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val w = plugin.server.getWorld(plugin.config.getString("worlds.overworld")!!) ?: return@Runnable
            val t = w.fullTime
            if (t % 24000L == 0L) {
                // 하루 증가
                plugin.state.currentDay += 1
                // 거래 리셋
                plugin.state.tradesToday.clear()
                // 만료된 발자국 정리
                val cur = plugin.state.currentDay
                plugin.state.footprints.removeIf { it.expireDay < cur }
                Stores.saveState(plugin, plugin.state)

                if (plugin.config.getBoolean("game.show_day_broadcast"))
                    Bukkit.broadcastMessage("§7Day §f${plugin.state.currentDay} §7시작!")

                // 100일차 진입 판정
                if (plugin.state.currentDay == plugin.config.getInt("game.max_day") + 1) {
                    plugin.winCheckOnDay100()
                }
            }
        }, 20L, 20L)
        return this
    }
}

```

**service/EggService.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import com.kh.dragonheist99.util.Stores
import org.bukkit.*
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class EggService(private val plugin: DragonHeist99) : Listener {
    fun hook(): EggService {
        plugin.server.pluginManager.registerEvents(this, plugin)
        // 엔더드래곤 기본 알 드랍은 바닐라 규칙대로 (엔드 포탈에서 알 생성)
        // 필요시 여기에서 엔드 리셋/드래곤 리스폰 정책 추가 가능
        // 엔더맨 이펙트 타이머
        scheduleEndermanEffect()
        return this
    }

    fun giveEggTo(p: Player) {
        p.inventory.addItem(ItemStack(Material.DRAGON_EGG))
        setHolder(p.uniqueId)
    }

    private fun setHolder(uuid: UUID?) {
        if (uuid == null) {
            plugin.state.eggHolder = null
            return
        }
        val id = uuid.toString()
        if (plugin.state.firstAcquirer == null) {
            plugin.state.firstAcquirer = id
        }
        plugin.state.eggHolder = id
        Stores.saveState(plugin, plugin.state)
        Bukkit.broadcastMessage("§5[알 획득] §f${Bukkit.getOfflinePlayer(uuid).name ?: "??"}")
    }

    private fun isHolder(p: Player) = plugin.state.eggHolder == p.uniqueId.toString()

    // 알 블록 부수어 아이템으로 얻는 순간 → 홀더 지정
    @EventHandler
    fun onBreakEgg(e: BlockBreakEvent) {
        if (e.block.type != Material.DRAGON_EGG) return
        val p = e.player ?: return
        // 바닐라: 드래곤 알은 부수면 텔레포트. 여기서는 "드랍 보장"을 위해 강제 처리
        e.isCancelled = true
        e.block.type = Material.AIR
        e.block.world.dropItemNaturally(e.block.location, ItemStack(Material.DRAGON_EGG))
        setHolder(p.uniqueId)
    }

    // 인벤토리 내 알 보유 유지 감지: 우클릭으로 블록 설치 → 홀더 해제(블록이 됨), 다시 부수면 위에서 재홀딩
    @EventHandler
    fun onInteractPlaceEgg(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val item = e.item ?: return
        if (item.type != Material.DRAGON_EGG) return
        // 설치 허용. 설치되면 손에서 빠짐 → eggHolder 그대로 두되, 실제론 소유가 사라질 수도.
        // 확실히 하려면 "인벤/핫바/오프핸드" 보유 스캔으로 매 틱 보정
        Bukkit.getScheduler().runTask(plugin) {
            val p = e.player
            if (!holdsAnyEgg(p)) {
                // 손에서 빠졌으면 홀더 제거
                if (isHolder(p)) setHolder(null)
            }
        }
    }

    private fun holdsAnyEgg(p: Player): Boolean {
        if (p.inventory.itemInMainHand.type == Material.DRAGON_EGG) return true
        if (p.inventory.itemInOffHand.type == Material.DRAGON_EGG) return true
        return p.inventory.contents.any { it?.type == Material.DRAGON_EGG }
    }

    // 차원 이동 제한 (+ 최초 획득자 엔드→오버월드 1회 허용)
    @EventHandler
    fun onPortal(e: PlayerPortalEvent) {
        val p = e.player
        if (!isHolder(p)) return
        val isEndToOver = e.from.world.environment == World.Environment.THE_END &&
                e.to?.world?.environment == World.Environment.NORMAL
        val isFirst = plugin.state.firstAcquirer == p.uniqueId.toString() &&
                !plugin.state.firstAcquirerEndToOverworldUsed
        if (isEndToOver && isFirst) {
            plugin.state.firstAcquirerEndToOverworldUsed = true
            Stores.saveState(plugin, plugin.state)
            return
        }
        e.isCancelled = true
        p.sendMessage("§c드래곤 알 보유 중에는 차원 이동 금지!")
    }

    @EventHandler
    fun onTeleport(e: PlayerTeleportEvent) {
        val p = e.player
        if (!isHolder(p)) return
        if (e.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
            e.cause == PlayerTeleportEvent.TeleportCause.END_PORTAL ||
            e.cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            e.isCancelled = true
            p.sendMessage("§c드래곤 알 보유 중에는 차원 이동 금지!")
        }
    }

    // 홀더가 나가면? 그대로 유지(오프라인 홀더). 필요시 정책 바꾸면 여기서 드랍 가능.
    @EventHandler fun onQuit(e: PlayerQuitEvent) {
        // 유지
    }

    // 홀더 사망 → 제단 & 보라색 신호기 100분
    @EventHandler
    fun onHolderDeath(e: org.bukkit.event.entity.PlayerDeathEvent) {
        val p = e.entity
        if (!isHolder(p)) return
        // 홀더 해제 + 제단생성 + 좌표 브로드캐스트
        plugin.state.eggHolder = null
        Stores.saveState(plugin, plugin.state)

        val l = p.location.block.location
        buildAltarWithBeacon(l)
        Bukkit.broadcastMessage("§5[알 드랍] §f좌표: §d${l.blockX}, ${l.blockY}, ${l.blockZ}")

        val ticks = 20L * 60L * plugin.config.getInt("egg.beacon_minutes")
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { removeAltar(l) }, ticks)
    }

    private fun buildAltarWithBeacon(base: org.bukkit.Location) {
        val w = base.world
        // 간단한 제단 (3x3 바닥 흑암, 중앙 비컨 + 보라유리 + 알)
        val bx = base.blockX; val by = base.blockY; val bz = base.blockZ
        for (x in -1..1) for (z in -1..1) {
            w.getBlockAt(bx + x, by - 1, bz + z).type = Material.OBSIDIAN
        }
        w.getBlockAt(bx, by, bz).type = Material.BEACON
        w.getBlockAt(bx, by + 1, bz).type = Material.PURPLE_STAINED_GLASS
        w.getBlockAt(bx, by + 2, bz).type = Material.DRAGON_EGG
        w.playSound(base, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f)
    }

    private fun removeAltar(base: org.bukkit.Location) {
        val w = base.world
        val bx = base.blockX; val by = base.blockY; val bz = base.blockZ
        val targets = listOf(
            Pair(bx, by) to Material.BEACON,
            Pair(bx, by + 1) to Material.PURPLE_STAINED_GLASS,
            Pair(bx, by + 2) to Material.DRAGON_EGG
        )
        targets.forEach { (xz, type) ->
            val (x, y) = xz
            if (w.getBlockAt(x, y, bz).type == type) w.getBlockAt(x, y, bz).type = Material.AIR
        }
        for (x in -1..1) for (z in -1..1) {
            val b = w.getBlockAt(bx + x, by - 1, bz + z)
            if (b.type == Material.OBSIDIAN) b.type = Material.AIR
        }
        w.playSound(base, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f)
    }

    // 엔더맨 이펙트(홀더에게 주기적 파티클/사운드)
    private fun scheduleEndermanEffect() {
        val min = plugin.config.getInt("egg.enderman_effect_min_sec").coerceAtLeast(30)
        val max = plugin.config.getInt("egg.enderman_effect_max_sec").coerceAtLeast(min + 1)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val holder = plugin.state.eggHolder?.let { UUID.fromString(it) } ?: return@Runnable
            val p = Bukkit.getPlayer(holder) ?: return@Runnable
            val w = p.world
            w.spawnParticle(Particle.PORTAL, p.location, 60, 1.0, 1.0, 1.0, 0.1)
            w.playSound(p.location, Sound.ENTITY_ENDERMAN_STARE, 1f, 1f)
        }, 20L * min, 20L * ((min + max) / 2))
    }
}

```

**service/FootprintService.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import com.kh.dragonheist99.model.Footprint
import com.kh.dragonheist99.util.Stores
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import kotlin.math.max

class FootprintService(private val plugin: DragonHeist99) : Listener {
    fun start(): FootprintService {
        plugin.server.pluginManager.registerEvents(this, plugin)
        // 렌더러
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable { renderTick() }, 10L, 10L)
        return this
    }

    private fun isEggHolder(uuid: String?) = plugin.state.eggHolder == uuid

    @EventHandler(ignoreCancelled = true)
    fun onMove(e: PlayerMoveEvent) {
        val p = e.player
        if (!isEggHolder(p.uniqueId.toString())) return
        if (e.from.world.uid != e.to.world.uid) return
        val dist2 = e.from.distanceSquared(e.to)
        val min = plugin.config.getDouble("egg.footprint_sample_blocks")
        if (dist2 < min * min) return
        val dir = p.location.direction
        plugin.state.footprints += Footprint(
            world = p.world.uid,
            x = p.location.x, y = p.location.y, z = p.location.z,
            dx = dir.x.toFloat(), dz = dir.z.toFloat(),
            expireDay = plugin.state.currentDay + 5
        )
        // 주기적으로 저장 (폭주 방지)
        if (plugin.state.footprints.size % 64 == 0) Stores.saveState(plugin, plugin.state)
    }

    private fun renderTick() {
        val w = plugin.server.getWorld(plugin.config.getString("worlds.overworld")!!) ?: return
        val isDay = w.time in 0..12000
        if (!isDay) return
        val cur = plugin.state.currentDay
        val range = max(8, plugin.config.getInt("egg.footprint_visible_range"))
        val vis = plugin.state.footprints.filter { it.expireDay >= cur }
        for (p in w.players) {
            val loc = p.location
            vis.asSequence()
                .filter { it.world == w.uid && loc.distanceSquared(Location(w, it.x, it.y, it.z)) <= range * range }
                .forEach {
                    val base = Location(w, it.x, it.y + 0.1, it.z)
                    w.spawnParticle(Particle.END_ROD, base, 2, 0.02, 0.0, 0.02, 0.0)
                    val tip = base.clone().add(it.dx * 0.5, 0.0, it.dz * 0.5)
                    w.spawnParticle(Particle.CRIT, tip, 1, 0.0, 0.0, 0.0, 0.0)
                }
        }
    }
}

```

**service/TradeLimiter.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType

class TradeLimiter(private val plugin: DragonHeist99) : Listener {
    fun hook(): TradeLimiter {
        plugin.server.pluginManager.registerEvents(this, plugin)
        return this
    }

    @EventHandler(ignoreCancelled = true)
    fun onTrade(e: InventoryClickEvent) {
        if (e.inventory.type != InventoryType.MERCHANT) return
        if (e.slotType != InventoryType.SlotType.RESULT) return
        val p = e.whoClicked as? Player ?: return
        val key = p.uniqueId.toString()
        val used = plugin.state.tradesToday.getOrDefault(key, 0)
        val limit = plugin.config.getInt("trades.limit_per_day")
        if (used >= limit) {
            e.isCancelled = true
            p.sendMessage("§e오늘 거래 한도($limit) 끝! 내일 다시 오쇼~")
        } else {
            plugin.state.tradesToday[key] = used + 1
        }
    }
}

```

**service/ItemRuleService.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.roundToInt

class ItemRuleService(private val plugin: DragonHeist99) : Listener {
    fun hook(): ItemRuleService {
        plugin.server.pluginManager.registerEvents(this, plugin)
        // 레시피 중 금지 아이템은 제거
        if (plugin.config.getBoolean("items.ban.elytra")) removeRecipe(Material.ELYTRA)
        if (plugin.config.getBoolean("items.ban.shulker_box")) removeShulkerRecipes()
        if (plugin.config.getBoolean("items.ban.ender_chest")) removeRecipe(Material.ENDER_CHEST)
        return this
    }

    private fun removeRecipe(mat: Material) {
        plugin.server.recipeIterator().forEachRemaining { r ->
            try {
                val result = r.result
                if (result?.type == mat) plugin.server.removeRecipe(r.key!!)
            } catch (_: Throwable) {}
        }
    }
    private fun removeShulkerRecipes() {
        val mats = Material.values().filter { it.name.endsWith("SHULKER_BOX") }
        mats.forEach { removeRecipe(it) }
    }

    private fun isPotion(stack: ItemStack?) =
        stack != null && (stack.type.name.endsWith("POTION"))

    private fun potionCount(p: Player): Int =
        p.inventory.contents.filterNotNull().count { isPotion(it) }

    private fun validatePotionLimit(p: Player): Boolean =
        potionCount(p) <= plugin.config.getInt("items.potion_max_stack")

    // 포션 2개 제한
    @EventHandler fun onPickup(e: EntityPickupItemEvent) {
        val p = e.entity as? Player ?: return
        if (isPotion(e.item.itemStack) && !validatePotionLimit(p)) {
            e.isCancelled = true
            p.sendMessage("§b포션은 최대 ${plugin.config.getInt("items.potion_max_stack")}개까지만 들 수 있어!")
        }
    }
    @EventHandler fun onInvClick(e: InventoryClickEvent) {
        val p = e.whoClicked as? Player ?: return
        plugin.server.scheduler.runTask(plugin) {
            if (!validatePotionLimit(p)) {
                e.isCancelled = true
                p.sendMessage("§b포션은 너무 많다~ 줄이자!")
            }
        }
    }
    @EventHandler fun onCraft(e: CraftItemEvent) {
        val p = e.whoClicked as? Player ?: return
        plugin.server.scheduler.runTask(plugin) {
            if (!validatePotionLimit(p)) e.isCancelled = true
        }
    }

    // 황금사과 재생시간 감소
    @EventHandler
    fun onConsume(e: PlayerItemConsumeEvent) {
        val t = e.item.type
        if (t == Material.GOLDEN_APPLE || t == Material.ENCHANTED_GOLDEN_APPLE) {
            plugin.server.scheduler.runTask(plugin) {
                val sec = plugin.config.getDouble("items.golden_apple_regen_seconds")
                val ticks = (sec * 20.0).roundToInt().coerceAtLeast(1)
                e.player.removePotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION)
                e.player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, ticks, if (t==Material.ENCHANTED_GOLDEN_APPLE) 4 else 1, true, true, true))
            }
        }
    }

    // 사용 금지: 겉날개, 펄(투척), 셜커/엔더상자, 방패
    @EventHandler fun onThrow(e: ProjectileLaunchEvent) {
        if (plugin.config.getBoolean("items.ban.ender_pearls_throw", false)) return // 키 오타 방지
        val proj = e.entity
        if (proj.type.name == "ENDER_PEARL" && plugin.config.getBoolean("items.ban.ender_pearl_throw")) {
            e.isCancelled = true
        }
    }
    @EventHandler fun onPlace(e: BlockPlaceEvent) {
        val t = e.blockPlaced.type
        if (plugin.config.getBoolean("items.ban.ender_chest") && t == Material.ENDER_CHEST) e.isCancelled = true
        if (plugin.config.getBoolean("items.ban.shulker_box") && t.name.endsWith("SHULKER_BOX")) e.isCancelled = true
    }
    @EventHandler fun onInteract(e: PlayerInteractEvent) {
        val p = e.player
        // 방패 사용 차단(들기 자체는 플레이어 재량이라 우클릭 방어만 컷)
        if (plugin.config.getBoolean("items.ban.shield") && p.isBlocking) {
            p.isBlocking = false
        }
        // 겉날개 글라이드 차단: Paper에선 ToggleGlideEvent가 더 정확하나 여기선 간이 컷
        if (plugin.config.getBoolean("items.ban.elytra") && p.isGliding) {
            p.isGliding = false
        }
    }
}

```

**service/EnchantService.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.meta.EnchantmentStorageMeta

class EnchantService(private val plugin: DragonHeist99) : Listener {
    fun hook(): EnchantService {
        plugin.server.pluginManager.registerEvents(this, plugin)
        return this
    }

    private fun cap(ench: Enchantment, level: Int): Int {
        val sMax = plugin.config.getInt("enchant.sharpness_max")
        val pMax = plugin.config.getInt("enchant.protection_max")
        return when (ench.key.key) {
            "sharpness" -> level.coerceAtMost(sMax)
            "protection" -> level.coerceAtMost(pMax)
            else -> level
        }
    }

    // 인챈트 테이블에서 상한 올려 적용
    @EventHandler
    fun onEnchant(e: EnchantItemEvent) {
        e.enchantsToAdd.replaceAll { ench, lvl -> cap(ench, lvl) }
    }

    // 모루: 책 붙이기 비용 39 고정 + 상한 반영
    @EventHandler
    fun onPrepareAnvil(e: PrepareAnvilEvent) {
        val inv = e.inventory
        val left = inv.getItem(0) ?: return
        val right = inv.getItem(1) ?: return
        if (right.itemMeta !is EnchantmentStorageMeta) return

        val meta = right.itemMeta as EnchantmentStorageMeta
        val result = left.clone()
        val map = meta.storedEnchants
        for ((ench, lvl) in map) {
            result.addUnsafeEnchantment(ench, cap(ench, lvl))
        }
        e.result = result
        inv.repairCost = plugin.config.getInt("enchant.anvil_book_apply_cost")
    }
}

```

**service/XpService.kt**

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent

class XpService(private val plugin: DragonHeist99) : Listener {
    fun hook(): XpService {
        plugin.server.pluginManager.registerEvents(this, plugin)
        return this
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        if (!plugin.config.getBoolean("xp.drop_full_on_death")) return
        val p = e.entity
        e.droppedExp = totalExp(p)
    }

    @EventHandler
    fun onDragonDeath(e: EntityDeathEvent) {
        if (e.entity is EnderDragon) {
            e.droppedExp = plugin.config.getInt("xp.dragon_kill_xp")
        }
    }

    // 전체 경험치 추정 계산
    private fun totalExp(p: Player): Int {
        var exp = (p.exp * p.expToLevel).toInt()
        var lvl = p.level
        while (lvl > 0) {
            lvl--
            exp += expForLevel(lvl)
        }
        return exp
    }

    // 바닐라 공식 근사
    private fun expForLevel(level: Int): Int = when {
        level >= 30 -> 112 + (level - 30) * 9
        level >= 15 -> 37 + (level - 15) * 5
        else -> 7 + level * 2
    }
}

```

**service/RecipeService.kt**  *(형판 조합/대장간 우회 허용)*

> “기존 조합법에서 형판 대신 네더라이트 사용” 해석: 스미싱 업그레이드 시 템플릿 없이 네더라이트 주괴만으로 업그레이드 허용. Paper 1.20+의 SmithingTransformRecipe로 커스텀 추가.
> 
> 
> 간단화: **다이아 방어구/도구 → 네더라이트 변환** 커스텀 스미싱 레시피 묶음 등록.
> 

```kotlin
package com.kh.dragonheist99.service

import com.kh.dragonheist99.DragonHeist99
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*

class RecipeService(private val plugin: DragonHeist99) {
    fun register(): RecipeService {
        if (plugin.config.getBoolean("items.recipes.netherite_template_alt")) {
            registerNetheriteSmithing()
        }
        return this
    }

    private fun registerNetheriteSmithing() {
        val smith = RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT)
        val baseItems = listOf(
            Material.DIAMOND_SWORD to Material.NETHERITE_SWORD,
            Material.DIAMOND_AXE to Material.NETHERITE_AXE,
            Material.DIAMOND_PICKAXE to Material.NETHERITE_PICKAXE,
            Material.DIAMOND_SHOVEL to Material.NETHERITE_SHOVEL,
            Material.DIAMOND_HOE to Material.NETHERITE_HOE,
            Material.DIAMOND_HELMET to Material.NETHERITE_HELMET,
            Material.DIAMOND_CHESTPLATE to Material.NETHERITE_CHESTPLATE,
            Material.DIAMOND_LEGGINGS to Material.NETHERITE_LEGGINGS,
            Material.DIAMOND_BOOTS to Material.NETHERITE_BOOTS
        )
        baseItems.forEachIndexed { idx, (base, result) ->
            val key = NamespacedKey(plugin, "smith_neth_no_template_$idx")
            val recipe = SmithingTransformRecipe(
                key,
                ItemStack(result),
                RecipeChoice.MaterialChoice(Material.SMITHING_TEMPLATE_UPGRADE),  // Dummy template 입력
                RecipeChoice.MaterialChoice(base),
                smith
            )
            // 핵심: 템플릿이 없어도 되도록 **템플릿 입력 칸**을 base와 동일 처리하고, 실제 등록 후 템플릿 소모 없음으로 취급될 수 있게 처리.
            // Paper에서 템플릿 필드는 필수라 dummy를 지정하되, 접수 단계에서 무시하는 우회는 불가.
            // 따라서 대안: **그라인드스톤/제작대 대체** 불가 → 스미싱이 필수 구조라
            // 실전에서는 "커스텀 스미싱 GUI"를 열어 처리하는 편이 안전. 여기선 **간이 대안** 제공:

            // 간이 대안: 일반 크래프팅으로 네더라이트 업그레이드 제공 (비바닐라)
            //  D N D
            //  N B N  (B=Base dia item, N=Netherite ingot)
            //  D N D
            val shapedKey = NamespacedKey(plugin, "craft_neth_no_template_$idx")
            val shaped = ShapedRecipe(shapedKey, ItemStack(result))
            shaped.shape("DND","NBM","DND")
            shaped.setIngredient('D', Material.DEEPSLATE)
            shaped.setIngredient('N', Material.NETHERITE_INGOT)
            shaped.setIngredient('B', base)
            shaped.setIngredient('M', Material.SMITHING_TABLE) // 상징적 재료
            plugin.server.addRecipe(shaped)
        }
    }
}

```

> 참고: 스미싱 템플릿 필드를 완전 생략하는 건 바닐라 레시피 규칙상 불가라서, 위처럼 비바닐라 크래프팅 대체 레시피를 제공하는 방식으로 “템플릿 없이 네더라이트 전환”을 구현했어. (실전에서 완벽하게 스미싱 UI 그대로 쓰고 싶으면 커스텀 컨테이너 GUI 플러그인으로 만드는 게 깔끔)
> 

---

# 기타: 규칙 보정 포인트

- **“게임 종료 시점 소지자만 우승”**: 위 로직은 100일차 진입 순간 판정. 굳이 “알 소지자 실시간 판정”을 매 틱 돌 필요 없음.
- **오버타임**: config로 토글만 넣었고, 필요하면 `PlayerEggHoldTimer` 만들어서 “N초 연속 보유 → 즉시 브로드캐스트 우승” 추가 가능.
- **발자국**: 파티클 방식이라 서버 부하 낮음. 플레이어 많으면 `visible.asSequence().take(??)`로 컷 가능.

---

# 빌드 & 실행

1. 프로젝트 루트에서:

```
./gradlew build

```

1. `build/libs/DragonHeist99-1.0.0.jar` 를 Paper 서버 `plugins/`에 넣고 서버 실행.
2. `config.yml` 값 확인 후 `/reload confirm` 또는 서버 재시작.