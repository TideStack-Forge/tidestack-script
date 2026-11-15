package com.ouroboros.script.intelligence;

import com.ouroboros.script.JavaScriptEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavascriptEditorEnvironmentTest {
  @Test void editorCatalogMatchesActualGraalExecutionAndContainsNoBrowserOrNodeEnvironment() throws Exception {
    try (var engine = new JavaScriptEngine()) {
      var globals = new ObjectMapper().readTree(engine.describeEditorGlobals());
      var names = new java.util.HashSet<String>(); globals.forEach(item -> names.add(item.path("name").asText()));
      for (String name : java.util.List.of("Java", "Polyglot", "Graal", "print", "load", "globalThis", "Array", "JSON")) {
        assertTrue(names.contains(name), name + " missing from actual runtime catalog");
        assertNotEquals("undefined", engine.execute("return typeof " + name, Map.of()).get());
      }
      for (String name : java.util.List.of("window", "document", "fetch", "XMLHttpRequest", "HTMLElement", "navigator", "localStorage", "setTimeout", "process", "require")) {
        assertFalse(names.contains(name), name + " leaked into catalog");
        assertEquals("undefined", engine.execute("return typeof " + name, Map.of()).get());
      }
      var javaBuiltin = java.util.stream.StreamSupport.stream(globals.spliterator(), false).filter(item -> item.path("name").asText().equals("Java")).findFirst().orElseThrow();
      assertTrue(javaBuiltin.path("members").toString().contains("type"));
      assertTrue(javaBuiltin.path("members").toString().contains("from"));
    }
    var snapshot = new RuntimeScriptIntelligenceSnapshotFactory(Map::of, null).build("javascript", "test-helper");
    assertTrue(new ObjectMapper().valueToTree(snapshot.getLanguageProjectionHints()).path("javascript").path("engineGlobals").isArray());
  }
}
