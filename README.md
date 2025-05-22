# plugin-douban

* 豆瓣管理插件, 支持在 Console 进行管理以及为主题端提供 `/douban` 页面路由。
* 提供从豆瓣爬取到的数据
* 内置模板，无需主题支持，但也可以通过主题自定义模板。

## 致谢
该插件是从[WP-Douban](https://fatesinger.com/101005)插件进行重构的。对贡献者表示感谢。

## 使用方式
* 在应用市场下载并启用。
* 启用插件之后会在 Console 的左侧添加一个`豆瓣`的菜单项，点击即可进入`豆瓣`管理页面。

## 📃文档
https://docs.kunkunyu.com/docs/plugin-douban

## 交流群
* 添加企业微信 （备注进群）
<img width="360" src="https://api.minio.yyds.pink/kunkunyu/files/2025/02/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20250212142105-pbceif.jpg" />

* QQ群
<img width="360" src="https://api.minio.yyds.pink/kunkunyu/files/2025/05/qq-708998089-iqowsh.webp" />

## 开发环境

```bash
git clone git@github.com:chengzhongxue/plugin-douban.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/plugin-douban.git
```

```bash
cd path/to/plugin-douban
```

```bash
# macOS / Linux
./gradlew pnpmInstall

# Windows
./gradlew.bat pnpmInstall
```

```bash
# macOS / Linux
./gradlew haloServer

# Windows
./gradlew.bat haloServer
```

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

修改 Halo 配置文件：

```yaml
halo:
  plugin:
    runtime-mode: development
    classes-directories:
      - "build/classes"
      - "build/resources"
    lib-directories:
      - "libs"
    fixedPluginPath:
      - "/path/to/plugin-douban"
```
