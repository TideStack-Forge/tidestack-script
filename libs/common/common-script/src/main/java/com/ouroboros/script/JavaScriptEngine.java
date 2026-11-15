package com.ouroboros.script;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import org.graalvm.polyglot.*;

import io.vavr.control.Try;

import com.ouroboros.util.Dates;
import com.ouroboros.util.SPI;

public class JavaScriptEngine extends AbstractScriptEngine implements ScriptRunner, ScriptCompiler, AutoCloseable {
  private static final String LANGUAGE = "js";
  private static final String JS_WRAPPER_START = """
      (function(){
      __contextClassesMap__.forEach((key, value) => this[key] = Java.type(value));
      """;
  private static final String JS_WRAPPER_END = "})()";
  private final Engine engine = Engine.newBuilder()
      .allowExperimentalOptions(true)
      .option("engine.WarnInterpreterOnly", "false")
      .build();

  private final HostAccess hostAccess = getHostAccess();

  @Override
  public String getLanguage() {
    return LANGUAGE;
  }

  private HostAccess getHostAccess() {
    var builder = HostAccess.newBuilder()
        .allowAllClassImplementations(true)
        .allowAllImplementations(true)
        .allowArrayAccess(true)
        .allowListAccess(true)
        .allowMapAccess(true)
        .allowPublicAccess(true);
    builder.targetTypeMapping(Value.class, LocalDateTime.class, Value::isInstant, v -> Dates.toLocalDateTime(v.asInstant()), HostAccess.TargetMappingPrecedence.HIGHEST);
    builder.targetTypeMapping(Value.class, Date.class, Value::isInstant, v -> Date.from(v.asInstant()), HostAccess.TargetMappingPrecedence.LOW);
    builder.targetTypeMapping(Value.class, List.class, Value::hasArrayElements, this::toJavaList, HostAccess.TargetMappingPrecedence.HIGHEST);
    builder.targetTypeMapping(Value.class, Set.class, Value::hasArrayElements, this::toJavaSet, HostAccess.TargetMappingPrecedence.HIGHEST);
    builder.targetTypeMapping(Value.class, Collection.class, Value::hasArrayElements, this::toJavaList, HostAccess.TargetMappingPrecedence.HIGH);
    builder.targetTypeMapping(Value.class, Map.class, Value::hasMembers, this::toJavaMap, HostAccess.TargetMappingPrecedence.HIGH);
    SPI.getServiceStream(JsHostAccessConfigurer.class)
        .forEach(configurer -> configurer.config(builder));
    return builder.build();
  }

  @Override
  public Try<Object> execute(String script, Map<String, Object> bindingsMap) {
    try (Context context = newContext()) {
      var contextMap = wrapContext(bindingsMap);
      fillBinding(context.getBindings("js"), contextMap);
      var wrappedScript = JS_WRAPPER_START + script + JS_WRAPPER_END;
      final var result = Try.of(() -> context.eval("js", wrappedScript));
      return remapResult(result);
    } catch (Exception e) {
      return Try.failure(e);
    }
  }

  @Override
  public Function<Map<String, Object>, Try<Object>> compile(String script) {
    try {
      final var wrappedScript = JS_WRAPPER_START + script + JS_WRAPPER_END;
      final Source source = Source.newBuilder("js", wrappedScript, "compiled_script_" + Dates.getTimestamp() + ".js").build();
      return bindingsMap -> {
        try (Context context = newContext()) {
          var contextMap = wrapContext(bindingsMap);
          fillBinding(context.getBindings("js"), contextMap);
          final var result = Try.of(() -> context.eval(source));
          return remapResult(result);
        } catch (Exception e) {
          return Try.failure(e);
        }
      };
    } catch (Exception e) {
      return contextMap -> Try.failure(new RuntimeException("编译脚本失败", e));
    }
  }

  private Context newContext() {
    return Context.newBuilder("js").engine(engine).allowAllAccess(true)
        .allowHostAccess(hostAccess).allowHostClassLookup(className -> true).build();
  }

  /** Describes engine builtins with the execution configuration, without evaluating user code or getters. */
  public String describeEditorGlobals() {
    try (Context context = newContext()) {
      return context.eval("js", """
          JSON.stringify(Object.entries(Object.getOwnPropertyDescriptors(globalThis)).map(([name, d]) => ({
            name, kind: typeof d.value,
            members: d.value != null && (typeof d.value === 'object' || typeof d.value === 'function')
              ? Object.entries(Object.getOwnPropertyDescriptors(d.value)).filter(([n, p]) => 'value' in p)
                .map(([name, p]) => ({name, kind: typeof p.value})) : []
          })))
          """).asString();
    }
  }

  @Override public void close() { engine.close(); }

  private Try<Object> remapResult(Try<Value> result) {
    var remapped = result
        .toEither()
        .map(this::toJavaObject)
        .mapLeft(e -> {
          if (e instanceof PolyglotException polyglotException) {
            var finalException = polyglotException.isHostException() ? polyglotException.asHostException() : polyglotException;
            return finalException;
          } else {
            return e;
          }
        });
    return remapped.isRight() ? Try.success(remapped.get()) : Try.failure(remapped.getLeft());
  }

  private void fillBinding(Value bindings, Map<String, Object> contextMap) {
    // TODO: Hack方法，待优化
    var contextClassesMap = new HashMap<String, String>();
    contextMap.forEach((key, value) -> {
      if (value instanceof Class<?> clazz) {
        contextClassesMap.put(key, clazz.getName());
      } else {
        bindings.putMember(key, value);
      }
    });
    bindings.putMember("__contextClassesMap__", contextClassesMap);
  }

  public Object toJavaObject(Value value) {
    if (value.isBoolean()) {
      return value.asBoolean();
    } else if (value.isNumber()) {
      if (value.fitsInByte()) {
        return value.asInt();
      } else if (value.fitsInShort()) {
        return value.asInt();
      } else if (value.fitsInInt()) {
        return value.asInt();
      } else if (value.fitsInLong()) {
        return value.asLong();
      } else if (value.fitsInFloat()) {
        return value.asDouble();
      } else if (value.fitsInDouble()) {
        return value.asDouble();
      } else {
        return value.as(Object.class);
      }
    } else if (value.isString()) {
      return value.asString();
    } else if (value.isDate()) {
      if (value.isHostObject()) {
        return value.asHostObject();
      }
      return Dates.toLocalDateTime(value.asInstant());
    } else if (value.hasArrayElements() && !value.isHostObject()) {
      return toJavaList(value);
    } else if (value.hasMembers() && !value.isHostObject()) {
      return toJavaMap(value);
    } else if (value.isNull()) {
      return null;
    } else {
      return value.asHostObject();
    }
  }

  private Map<String, Object> toJavaMap(Value value) {
    var keys = value.getMemberKeys();
    if (keys.size() == 0) {
      return Collections.emptyMap();
    }

    var map = new LinkedHashMap<String, Object>();
    keys.forEach(key -> map.put(key, toJavaObject(value.getMember(key))));
    return map;
  }

  private List<Object> toJavaList(Value value) {
    var size = value.getArraySize();
    if (size == 0) {
      return Collections.emptyList();
    }

    var list = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      list.add(toJavaObject(value.getArrayElement(i)));
    }
    return list;
  }

  private Set<Object> toJavaSet(Value value) {
    var size = value.getArraySize();
    if (size == 0) {
      return Collections.emptySet();
    }

    var set = new HashSet<>();
    for (int i = 0; i < size; i++) {
      set.add(toJavaObject(value.getArrayElement(i)));
    }
    return set;
  }
}
