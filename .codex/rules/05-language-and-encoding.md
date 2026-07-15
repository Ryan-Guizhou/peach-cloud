# Language And Encoding

## Java And Framework Compatibility

`REQUIRED`：

- Java 源码兼容 Java 8；禁止 `var`、`record`、text block、switch expression、pattern matching 等 Java 9+ 语法。
- 禁止 `List.of`、`Map.of`、`Stream.toList`、`Optional.isEmpty`、`Files.readString`、`Path.of` 等 Java 9+ API。
- Spring Boot 按 `2.7.13`，Spring Cloud 按 `2021.0.5`，Spring Cloud Alibaba 按 `2021.0.5.0` 编写。
- 不确定外部 API 时查询当前依赖源码、仓库范式或 `context7`，不得凭记忆生成。

## UTF-8 Without BOM

`REQUIRED`：

- 所有源码、Markdown、SQL、脚本、配置和前端文本必须是严格 UTF-8，且文件开头不得包含 UTF-8 BOM（`EF BB BF`）。
- 禁止 UTF-16、GBK、ANSI、系统默认编码以及任何带 BOM 的 UTF-8 文本。
- 新建文件默认 LF；`.bat`、`.cmd` 按 `.editorconfig`/`.gitattributes` 使用 CRLF。编码要求和换行要求彼此独立。
- 读写中文文件时必须显式指定 UTF-8；禁止使用会按系统默认编码整文件读写的 PowerShell、Java、Python、Node 或编辑器操作。
- 修改前先严格解码目标文件；无法严格解码时停止写入并报告，不得通过替换字符或全量重写掩盖损坏。
- 修改后运行 `node scripts/check-utf8.mjs`。若仅需清理现存 UTF-8 BOM，可运行 `node scripts/check-utf8.mjs --fix-bom`，该命令不得转换其他编码。

`FORBIDDEN`：

- 将控制台乱码复制回源码或文档。
- 出现 Unicode replacement character（`U+FFFD`）或检查脚本定义的其他已知乱码标记。
- 为局部编辑顺带改变无关文件正文、换行或格式。
- 对无法识别的原始字节做猜测性转码。

## Verification

编码验证以文件字节为准，不以终端显示为准：

```bash
node scripts/check-utf8.mjs
mvn validate -Pdevelopment
```

Maven `validate` 已接入同一脚本；若 Node.js 不可用，构建应失败而不是跳过编码门禁。终端显示乱码时，先运行脚本确认文件本体，再调整控制台编码。
