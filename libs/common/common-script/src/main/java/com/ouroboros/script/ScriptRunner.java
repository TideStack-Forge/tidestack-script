package com.ouroboros.script;

import java.util.Map;

import io.vavr.control.Try;

public interface ScriptRunner {
  String getLanguage();

  Try<Object> execute(String script, Map<String, Object> bindings);
}
