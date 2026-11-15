package com.ouroboros.script;

import java.util.Map;
import java.util.function.Function;

import io.vavr.control.Try;

public interface ScriptCompiler {
  String getLanguage();

  Function<Map<String, Object>, Try<Object>> compile(String script);
}
