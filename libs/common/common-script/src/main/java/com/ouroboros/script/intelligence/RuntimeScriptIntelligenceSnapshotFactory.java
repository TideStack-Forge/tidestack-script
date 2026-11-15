package com.ouroboros.script.intelligence;

import java.util.Map;
import com.ouroboros.codeintelligence.runtime.EnvironmentSnapshotFactory;

/** Projects the execution contributors; reflection is shared with expression authoring. */
public class RuntimeScriptIntelligenceSnapshotFactory {
  private final EnvironmentSnapshotFactory factory;
  private volatile java.util.List<?> javascriptGlobals;

  public RuntimeScriptIntelligenceSnapshotFactory(ScriptContextCatalog catalog, JavaTypeMetadataIndex types) {
    factory = new EnvironmentSnapshotFactory(catalog == null ? Map::of : catalog::getGlobals, types);
  }

  public ScriptIntelligenceSnapshot build(String language, String source) {
    var snapshot = factory.build(language, toRuntimeProfile(language), source);
    if ("javascript".equalsIgnoreCase(language)) {
      var hints = new java.util.LinkedHashMap<String, Object>(snapshot.getLanguageProjectionHints());
      hints.put("javascript", Map.of("runtimeProfile", snapshot.getRuntimeProfile(), "excludeBrowserGlobals", true,
          "engineGlobals", javascriptGlobals()));
      snapshot.setLanguageProjectionHints(hints);
      snapshot.setVersionToken(com.ouroboros.codeintelligence.runtime.ContentVersion.of(java.util.List.of(snapshot.getVersionToken(), hints)));
    }
    return snapshot;
  }

  private synchronized java.util.List<?> javascriptGlobals() {
    if (javascriptGlobals == null) try (var engine = new com.ouroboros.script.JavaScriptEngine()) {
      javascriptGlobals = com.ouroboros.util.Json.toBean(java.util.List.class, engine.describeEditorGlobals());
    }
    return javascriptGlobals;
  }

  public String toRuntimeProfile(String language) {
    if ("javascript".equalsIgnoreCase(language)) return "logicflow-graaljs";
    if ("groovy".equalsIgnoreCase(language)) return "logicflow-groovy";
    throw new IllegalArgumentException("Unsupported script language: " + language);
  }
}
