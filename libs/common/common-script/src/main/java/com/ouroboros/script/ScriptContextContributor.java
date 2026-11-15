package com.ouroboros.script;

import java.util.Map;

public interface ScriptContextContributor {
  Map<String, Object> getBindings();
}
