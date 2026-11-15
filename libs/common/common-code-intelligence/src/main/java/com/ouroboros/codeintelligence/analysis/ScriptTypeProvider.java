package com.ouroboros.codeintelligence.analysis;

import java.util.Map;
import java.util.Set;
import com.ouroboros.codeintelligence.type.TypeShape;

/** Language-owned, static source analysis. Implementations must not execute code or load application classes. */
public interface ScriptTypeProvider {
  Set<String> languages();
  TypeShape inferOutput(String source, Map<String, TypeShape> bindings);
}
