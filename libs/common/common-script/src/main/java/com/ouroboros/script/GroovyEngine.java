package com.ouroboros.script;

import java.util.Map;
import java.util.function.Function;

import io.vavr.control.Try;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class GroovyEngine extends AbstractScriptEngine implements ScriptRunner, ScriptCompiler {
  private static final String LANGUAGE = "groovy";

  @Override
  public String getLanguage() {
    return LANGUAGE;
  }

  @Override
  public Try<Object> execute(String script, Map<String, Object> bindingsMap) {
    var context = wrapContext(bindingsMap);
    // 执行Groovy脚本
    final var bindings = new OrElseNullBinding(context);
    final var shell = new GroovyShell(bindings);
    return Try.of(() -> shell.evaluate(script));
  }

  @Override
  public Function<Map<String, Object>, Try<Object>> compile(String script) {
    // TODO: 编译执行Groovy脚本
    // 编译后的 Script 对象不是线程安全的，因此需要考虑用一个策略
    // 建立一个 Script 对象池，每次执行脚本时，从池中取出一个 Script 对象，执行完毕后，再放回池中。
    return bindingsMap -> execute(script, bindingsMap);
  }

  private static class OrElseNullBinding extends Binding {
    public OrElseNullBinding(Map variables) {
      super(variables);
    }

    @Override
    public Object getVariable(String name) {
      if (hasVariable(name)) {
        return super.getVariable(name);
      } else {
        return null;
      }
    }
  }
}
