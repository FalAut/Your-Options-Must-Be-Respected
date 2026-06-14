# Your Options Must Be Respected

让整合包自带的默认选项和配置，在 Minecraft 正式读取它们之前就被正确写入。

**Your Options Must Be Respected** 是一个面向整合包的工具模组。它会在游戏启动早期扫描整合包提供的默认文件，并把它们复制到真实的游戏目录或 `config` 目录中，适合用来分发默认按键、画面、音量、辅助功能设置，以及其他只应在首次启动时写入的配置文件。

本模组是 **shedaniel** 制作的 **Your Options Shall Be Respected** 的移植版本，在保留默认配置同步思路的基础上，针对 `options.txt` 被启动器提前创建的场景进行了调整。

## 制作目的

制作这个模组的主要目的，是适配部分启动器会在游戏正式启动前为了自动设置语言而提前创建 `options.txt` 的情况。此时，许多同类默认选项同步模组会因为检测到 `options.txt` 已经存在而选择跳过，导致整合包作者提供的默认选项无法生效。

为了解决这个问题，本模组加入了 `options_applied.flag` 设计：首次应用默认 `options.txt` 后写入标记文件。这样既可以在没有标记文件时替换启动器提前生成的 `options.txt`，也可以在后续启动中保留玩家已经修改过的设置，避免反复重置。

## 适用场景

这个模组适合整合包作者使用，例如：

- 想让玩家第一次启动整合包时自动获得推荐画质、音量、语言或按键设置
- 想预置某些客户端或服务端配置，但不想每次更新整合包时覆盖玩家修改
- 想解决启动器提前生成 `options.txt`，导致整合包默认选项无法生效的问题
- 想把默认配置文件放在整合包目录里随包发布，而不是要求玩家手动复制

## 使用方法

把默认文件放进：

```text
.minecraft/config/yosbr/
```

文件会按下面的规则同步：

```text
.minecraft/config/yosbr/options.txt
=> .minecraft/options.txt
```

```text
.minecraft/config/yosbr/config/example.toml
=> .minecraft/config/example.toml
```

```text
.minecraft/config/yosbr/resourcepacks/example.zip
=> .minecraft/resourcepacks/example.zip
```

也就是说：

- `config/yosbr/config/` 里的文件会复制到真实的 `.minecraft/config/`
- `config/yosbr/` 下其他路径会按相同相对路径复制到 `.minecraft/`

## 覆盖规则

`options.txt` 有特殊处理：

- 如果玩家还没有 `options.txt`，模组会复制默认文件
- 如果已经存在 `options.txt`，但还没有 `options_applied.flag`，模组会认为它可能是启动器提前生成的文件，并用默认文件替换一次
- 替换或复制成功后会写入 `options_applied.flag`
- 之后再次启动时，只要标记文件存在，就不会继续覆盖玩家的 `options.txt`

其他普通文件的规则更简单：

- 目标文件不存在时复制
- 目标文件已经存在时保留，不覆盖

## 给整合包作者的建议

- 只把真正需要作为默认值的文件放进 `config/yosbr/`
- 发布前用一个全新的实例测试首次启动效果
- 修改默认 `options.txt` 后，如果测试实例里已经存在 `options_applied.flag`，需要删除该标记文件再测试首次应用流程

> **注意事项**  
> 在提供默认 `options.txt` 时，请确保文件中包含正确的 `version` 键（即当前 Minecraft 版本对应的数据版本，例如 1.21.1 为 `3955`）。  
> 根据 Minecraft 官方机制，如果 `options.txt` 缺少 `version` 字段，游戏会在启动时丢弃该文件并重新生成默认选项，导致整合包预设的选项失效。  
> 你可以在 [Minecraft Wiki](https://minecraft.wiki/w/Options.txt) 查阅各版本对应的数据版本号，或从正常启动过的客户端中获取正确的 `version` 行。
