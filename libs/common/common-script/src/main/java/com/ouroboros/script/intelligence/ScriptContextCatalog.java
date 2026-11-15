package com.ouroboros.script.intelligence;

import java.util.Map;

public interface ScriptContextCatalog {
  Map<String, Object> getGlobals();
}
