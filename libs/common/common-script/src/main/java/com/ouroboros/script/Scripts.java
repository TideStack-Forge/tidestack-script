package com.ouroboros.script;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.vavr.control.Try;

import com.ouroboros.util.SPI;

public final class Scripts {
  private final static Scripts INSTANCE = new Scripts();
  private final Map<String, ScriptRunner> runnerMap = new HashMap<>();
  private final Map<String, ScriptCompiler> compilerMap = new HashMap<>();

  private Scripts() {
    var jsEngine = new JavaScriptEngine();
    var groovyEngine = new GroovyEngine();
    runnerMap.put("js", jsEngine);
    runnerMap.put("javascript", jsEngine);
    runnerMap.put("groovy", groovyEngine);
    compilerMap.put("js", jsEngine);
    compilerMap.put("javascript", jsEngine);
    compilerMap.put("groovy", groovyEngine);
    SPI.getReversedServiceStream(ScriptRunner.class).forEach(runner -> runnerMap.put(runner.getLanguage(), runner));
    SPI.getReversedServiceStream(ScriptCompiler.class).forEach(compiler -> compilerMap.put(compiler.getLanguage(), compiler));

  }

  public static Function<Map<String, Object>, Try<Object>> compile(String language, String script) {
    return INSTANCE.doCompile(language, script);
  }

  public static Try<Object> execute(String language, String script, Map<String, Object> bindingsMap) {
    return INSTANCE.doExecute(language, script, bindingsMap);
  }

  Function<Map<String, Object>, Try<Object>> doCompile(String language, String script) {
    var compiler = compilerMap.get(language);
    if (compiler == null) {
      return (bindings) -> Try.failure(new IllegalArgumentException("Unsupported language: " + language));
    }
    return compiler.compile(script);
  }

  Try<Object> doExecute(String language, String script, Map<String, Object> bindingsMap) {
    var runner = runnerMap.get(language);
    if (runner == null) {
      return Try.failure(new IllegalArgumentException("Unsupported language: " + language));
    }
    return runner.execute(script, bindingsMap);
  }
}
