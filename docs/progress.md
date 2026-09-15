# 开发进度

按日期追加，记录完成项、验证结果和下一步。当前阶段：开发环境已跑通，开始准备首个玩法原型。

## 2026-09-14｜开发环境与首次运行

### 已完成

- 承接 9 月 13 日的立项：原作世界观与序列晋升体系，结合原创剧情，以养成、战斗、探索为核心；资料范围包含两部小说。
- 选择 VS Code，创建 `core/` NeoForge MDK 工程。版本：MC 1.21.1、NeoForge 21.1.250、JDK 21、Gradle 9.2.1、ModDevGradle 2.0.147。
- 使用本地 ZIP 解决 Gradle 发行包下载问题；在个人 Gradle 配置中设置代理，解决后续依赖下载的连接问题。代理与 Java 安装路径不加入仓库。
- 用户完成构建及客户端运行；检查确认产生 `core/build/libs/examplemod-1.0.0.jar`（9,990 字节，生成于 00:28）。JAR 内包含示例类、模组描述和语言资源。
- 运行日志确认加载 Example Mod 1.0.0，使用 Java 21.0.12；00:39 进入单人世界，随后保存全部维度并正常退出。
- 为上传 GitHub 添加根目录忽略规则，恢复 Wrapper 官方下载地址，将构建工作流移至根目录 `.github/workflows/` 并指定 `core/` 为构建目录。
- 恢复官方地址后执行 `gradlew.bat --version` 成功，使用已准备的本地缓存启动 Gradle 9.2.1，无需重新联网下载；检查忽略规则后保留 21 个候选提交文件，文档内部链接检查通过。

### 检查结论与待办

| 项目 | 状态 |
| --- | --- |
| 构建并生成示例 JAR | 已完成：用户构建成功，产物已检查 |
| 示例 Mod 加载、进入单人世界、保存退出 | 已由现有日志确认；本轮未进行游戏画面目视检查 |
| 示例方块与物品模型 | 待补充：日志提示 `example_block` 的方块状态/模型及 `example_item` 物品模型缺失，源码资源目录也未提供对应文件 |
| 独立服务端与多人联机 | 尚未验证；单人内置服务端不等于独立服务端测试 |
| 正式 Mod 名称、包名、ID | 尚未确定，目前仍为 `examplemod` 模板 |
| 序列、魔药、技能、扮演消化 | 尚未实现 |
| GitHub 仓库和远程构建 | 尚未创建/上传，工作流尚未在 GitHub 执行 |

日志中未发现 ERROR/FATAL 级记录，但有模型、声音等 WARN；因此当前结论是“开发链路跑通”，不是“内容和资源已经完善”。

### 下一步

1. 将源码与文档上传 GitHub，保留当前可运行的基础版本。
2. 确定正式 Mod 名称和 ID，清理示例内容并补齐基础资源。
3. 选定首条途径，制作一个原创任务与魔药/基础技能原型，再逐步加入消化和晋升。
4. 验证独立服务端及多人状态保存。

## GitHub 提交范围

以整个项目根目录为一个仓库，`core/` 是其中的开发工程。

| 应提交 | 不提交 |
| --- | --- |
| 根目录 README、`.gitignore`、`docs/` | `.gradle/`、`build/`、运行实例 `run/`、存档和日志 |
| `core/src/` 中自写代码、资源与模组描述模板 | Minecraft 本体、开发依赖、Gradle 发行 ZIP |
| `core/build.gradle`、`settings.gradle`、`gradle.properties` | 个人代理、绝对 Java 路径、令牌和密码 |
| `core/gradlew`、`gradlew.bat`、`gradle/wrapper/`（含 Wrapper JAR） | 编译出的 Mod JAR；正式发版时可作为 GitHub Release 附件 |
| `.github/workflows/build.yml` | 临时 IDE 文件和机器专用设置 |
| `core/.gitattributes`、`.gitignore`、README、`TEMPLATE_LICENSE.txt` | 未获许可的外部图片、音效和第三方模组二进制 |

模板的 MIT 许可文件应保留；它仅说明模板的许可，不代表原作、未来代码和第三方素材已统一采用 MIT。

GitHub 新建一个空仓库，不额外生成 README、许可证或 `.gitignore`。本地首次操作：

```powershell
cd E:\Code\Lord-Of-MysMc
git init -b main
git add .
git diff --cached --stat
git status
git commit -m "Initialize NeoForge 1.21.1 project and progress notes"
git remote add origin https://github.com/YOUR_USERNAME/Lord-Of-MysMc.git
git push -u origin main
```

将 `YOUR_USERNAME` 和仓库名替换成自己创建的仓库地址。提交前确认文件列表没有缓存、运行数据或私人配置；如果 Git 提示身份未配置，再用仓库级 `git config user.name` 与 `git config user.email` 设置，可使用 GitHub 提供的 noreply 邮箱。首次推送按 Git 凭据管理器提示登录。

后续更新使用 `git add .`、检查暂存内容、`git commit -m "本次变更说明"`、`git push`。本轮仅准备文件和说明，没有替用户创建远程仓库或推送。

参考：[GitHub 官方上传指南](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)。
