# Starforge（星辉工坊）

一个 **Minecraft 1.21.1 / NeoForge 21.1.x** 模组，包含「轻语轻扬」功能道具以及一套星辉工具 / 盔甲。

> 注册体系、网络包、按键绑定、GUI、贴图、模型、配方、掉落表、语言文件都已经打通，
> 你可以在这个骨架上继续加方块、机器、生物或玩法。

## 已实现内容

| 类型 | 内容 |
| --- | --- |
| 功能道具 | **轻语轻扬** `whisper`（按 G 打开开关界面：无敌 / 飞行 / 一击必杀 / 夜视 / 饱和） |
| 物品 | 星辉锭 `starforge_ingot` |
| 方块 | 星辉金属块 `starforge_block`（可被镐采集、有掉落表） |
| 工具 | 星辉剑 / 镐 / 斧 / 锹 / 锄（自定义 Tier：2400 耐久、挖掘速度 10、附魔性 22、可挖相当于下界合金等级的方块） |
| 盔甲 | 星辉头盔 / 胸甲 / 护腿 / 靴子（自定义 ArmorMaterial：护甲值 4/9/7/4、韧性 3、击退抗性 0.1） |
| 创造模式 | 独立标签页「星辉工坊」 |
| 数据 | 12 个合成配方 + 方块掉落表 + 中英文语言文件 |

## 轻语轻扬（`starforge:whisper`）

**合成**（工作台 3×3，1 根木棍 + 1 个工作台 + 3 块任意木板）：

```
 .  木棍  .
木板 工作台 木板
 .  木板  .
```

**使用**：主手拿着它按 **G** 打开界面，界面里 5 个按钮就是 5 个开关，点一下在「开 / 关」之间切换。

**能力**（只在主手拿着它、且对应开关打开时生效；放下物品立刻失效）：

| 开关 | 效果 |
| --- | --- |
| 无敌 | 完全免疫伤害（取消 `LivingHurtEvent`） |
| 飞行 | 获得创造模式飞行能力，关掉开关或放下物品后立刻收回 |
| 一击必杀 | 手持攻击时伤害抬到 1000，基本一刀一个 |
| 夜视 | 持续夜视 |
| 饱和 | 持续饱和，饱食度不掉 |

**实现位置**：

* 物品与开关数据：`com/starforge/whisper/WhisperItem.java`、`WhisperData.java`、`WhisperAbility.java`
* 服务端效果：`com/starforge/whisper/WhisperEvents.java`
* 按键与界面：`com/starforge/client/ClientSetup.java`、`WhisperScreen.java`
* 客户端→服务端同步：`com/starforge/network/`（`TogglePayload` + `ModNetwork`）

改键位在 `ClientSetup` 的 `GLFW.GLFW_KEY_G`；想加新能力，在 `WhisperAbility` 末尾追加枚举项，再到
`WhisperEvents` 里处理即可（界面按钮会自动多出来一个）。图标是 `assets/starforge/textures/item/whisper.png`，
由你提供的图居中裁成正方形后缩放到 128×128。

## 环境要求

* **JDK 21**（NeoForge 1.21.1 强制要求，JDK 8/17 都不行）
* 网络可以访问 `maven.neoforged.net` 与 `services.gradle.org`
* 推荐 IntelliJ IDEA 2024.2 或更新版本

## 构建 / 运行

Windows：

```bat
gradlew.bat build        :: 产物在 build\libs\starforge-1.0.0+mc1.21.1.jar
gradlew.bat runClient    :: 启动带模组的客户端
gradlew.bat runServer    :: 启动服务端
```

Linux / macOS：

```sh
./gradlew build
./gradlew runClient
```

用 IDEA：直接 `File -> Open` 选择本目录，等 Gradle 同步完成后运行 `runClient` 配置即可。

> 首次同步会下载 NeoForge、Minecraft 反编译源码等，通常需要几分钟。

## 目录结构

```
starforge/
├─ build.gradle                  # ModDevGradle 2 构建脚本
├─ settings.gradle               # 仓库配置（NeoForged + Maven Central）
├─ gradle.properties             # 版本号 / mod 元数据 / 代理开关
├─ gradlew(.bat)                 # Gradle Wrapper，不用自己装 Gradle
├─ tools/generate_assets.py      # 贴图与 JSON 资源生成脚本（可选，改配色后可重跑）
└─ src/main/
   ├─ java/com/starforge/
   │  ├─ Starforge.java          # @Mod 主类，把各个 DeferredRegister 挂到事件总线
   │  └─ registry/
   │     ├─ ModBlocks.java       # 方块
   │     ├─ ModItems.java        # 物品、工具、盔甲
   │     ├─ ModToolTiers.java    # 工具材质（Tier）
   │     ├─ ModArmorMaterials.java # 盔甲材质（ArmorMaterial）
   │     └─ ModCreativeTabs.java # 创造模式标签页
   └─ resources/
      ├─ META-INF/neoforge.mods.toml
      ├─ pack.mcmeta
      ├─ assets/starforge/...    # 贴图、模型、方块状态、语言文件
      └─ data/starforge/...      # 配方、掉落表
```

## 加一个新物品（最短路径）

1. `ModItems` 里加一行：

   ```java
   public static final DeferredItem<Item> STAR_DUST = ITEMS.registerSimpleItem("star_dust");
   ```

2. `ModCreativeTabs` 的 `displayItems` 里加 `output.accept(ModItems.STAR_DUST.get());`
3. 放两张资源：
   * `assets/starforge/textures/item/star_dust.png`（16×16）
   * `assets/starforge/models/item/star_dust.json`：
     ```json
     { "parent": "minecraft:item/generated", "textures": { "layer0": "starforge:item/star_dust" } }
     ```
4. `assets/starforge/lang/en_us.json` 与 `zh_cn.json` 里加 `"item.starforge.star_dust": "..."`

工具只需要把 `new SwordItem(...)` 换成你的 `Tier`；盔甲只需要把 `ArmorItem` 的第一个参数换成你的 `ArmorMaterial`。
注意盔甲材质要在 `assets/starforge/textures/models/armor/<材质名>_layer_1.png` 和 `_layer_2.png` 放贴图。

## 关于网络（重要）

`maven.neoforged.net` 在国内基本连不上。本工程已经内置了两层处理：

1. `build.gradle` 里优先使用 ForgeCDN 镜像 `https://neoforged.forgecdn.net/releases`；
2. `gradle/mirror.init.gradle` 会把 ModDevGradle 内部自己添加的官方仓库地址也改写到镜像：

   ```bat
   gradlew.bat build -I gradle\mirror.init.gradle
   ```

   用 IDEA 的话，在 `Settings -> Build Tools -> Gradle -> 初始化脚本`（或命令行 `-I`）里加上这个文件。

**当前已知的遗留问题**：镜像里缺 `net.neoforged:minecraft-dependencies:1.21.1`，
而 NeoFormRuntime 在反编译 Minecraft 之后会直接从 `maven.neoforged.net` 下载十来个库 jar
（checker-qual、guava、log4j 等），这一步在没有代理的情况下会卡住。
所以本机跑通完整构建需要一个能访问 `maven.neoforged.net` 的代理/VPN。

如果 Gradle 报 `Could not resolve`，可以把 `gradle.properties` 里注释掉的
`systemProp.https.proxyHost/Port` 四行打开，改成你自己的代理端口。

## 版本号修改

模组版本在 `gradle.properties` 的 `mod_version`，NeoForge 版本在 `neo_version`，mod id 在 `mod_id`（改 id 时记得同步
`Starforge.MOD_ID`、`assets`/`data` 目录名和 `neoforge.mods.toml` 里的引用）。
