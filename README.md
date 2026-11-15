# TideStack Script

可在平台之外使用的 Groovy / JavaScript 执行、Spring EL 表达式求值和 FreeMarker 文本模板组件。需要 Java 21 和 Maven 3.8.6+。

```bash
mvn clean verify
```

| Maven artifact（groupId 为 `com.ouroboros`） | 入口 |
| --- | --- |
| `common-script` | `com.ouroboros.script.Scripts` |
| `common-expression` | `com.ouroboros.expression.EL` |
| `common-template` | `com.ouroboros.template.TextTemplateEngine` |
| `common-code-intelligence` | 上述模块共用的类型与代码上下文契约 |
| `ouroboros-util` | 上述模块所需的基础工具与 SPI 加载 |

版本与平台保持一致，以根 `pom.xml` 为准。本仓库包含完整源码依赖闭包，构建无需平台私有父 POM 或私有 Maven 仓库。构建成功后可用 `mvn install` 安装到本地 Maven 仓库；以上坐标不代表已经发布到 Maven Central。

```java
import java.util.Map;
import com.ouroboros.script.Scripts;
import com.ouroboros.expression.EL;
import com.ouroboros.template.TextTemplateEngine;

var result = Scripts.execute("groovy", "price * count", Map.of("price", 12, "count", 3));
System.out.println(result.get()); // 36
System.out.println(Scripts.execute("js", "return price * count", Map.of("price", 12, "count", 3)).get());
System.out.println(EL.eval("${price * count}", Map.of("price", 12, "count", 3)).get());
System.out.println(new TextTemplateEngine().render("Hello ${name}", Map.of("name", "TideStack")));
```

脚本执行适用于可信脚本，JavaScript 允许访问宿主 Java 类，Groovy 同样具有宿主权限。执行外部用户提交的脚本需要额外的进程隔离和资源限制。`Scripts.compile` 返回可重复调用的函数；Groovy 当前每次调用重新执行脚本，不承诺预编译缓存。

新增语言通过 `ScriptRunner` / `ScriptCompiler`，新增全局上下文通过 `ScriptContextContributor` / `ExpressionContextContributor`，均在 `META-INF/services` 注册。保留原有 SPI 加载顺序和 Java 包名。

本仓库由 TideStack 总仓的 `scripts/open-source/export-component.py --component script` 单向生成。源码修复、版本调整和测试先进入总仓，再同步到本仓库；`ouroboros-util` 与 `common-code-intelligence` 随分发提供构建支持，使用同一份源码和版本。制品版本和发布统一由总仓管理，本镜像不独立演进或重复发版。许可证随总仓发布决策统一确定，当前为本地发布候选。
