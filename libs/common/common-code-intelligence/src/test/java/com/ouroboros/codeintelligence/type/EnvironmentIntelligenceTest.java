package com.ouroboros.codeintelligence.type;

import static org.junit.jupiter.api.Assertions.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.ouroboros.codeintelligence.runtime.EnvironmentSnapshotFactory;
import com.ouroboros.script.intelligence.*;

class EnvironmentIntelligenceTest {
  public static class Tools {
    public String name;
    public static String VERSION;
    public List<String> names() { throw new AssertionError("Must not invoke tool"); }
    public static int count(String value) { throw new AssertionError("Must not invoke tool"); }
  }

  @Test void reflectsSignaturesAndSeparatesClassMembersWithoutReadingValues() {
    var javaType = new ScriptIntelligenceJavaType();
    javaType.setQualifiedName("test.Tools"); javaType.setSimpleName("Tools");
    JavaTypeMetadataIndex index = new JavaTypeMetadataIndex() {
      public List<String> getImportCandidates() { return List.of("test.Tools"); }
      public List<ScriptIntelligenceJavaType> getTypes() { return List.of(javaType); }
      public Optional<ScriptIntelligenceJavaType> findBySimpleName(String name) { return Optional.of(javaType); }
      public Optional<ScriptIntelligenceJavaType> findByQualifiedName(String name) { return Optional.of(javaType); }
    };
    var bindings = new LinkedHashMap<String, Object>();
    bindings.put("tool", new Tools()); bindings.put("Tools", Tools.class); bindings.put("empty", null);
    var factory = new EnvironmentSnapshotFactory(() -> bindings, index);
    var snapshot = factory.build("javascript", "graaljs", "helper");
    var instance = snapshot.getGlobals().stream().filter(item -> item.getName().equals("tool")).findFirst().orElseThrow();
    assertEquals(List.of("name", "names"), instance.getMembers().stream().map(ScriptIntelligenceMember::getName).toList());
    assertEquals("string", instance.getMembers().get(1).getShape().element().type());
    var type = snapshot.getGlobals().stream().filter(item -> item.getName().equals("Tools")).findFirst().orElseThrow();
    assertEquals(List.of("VERSION", "count"), type.getMembers().stream().map(ScriptIntelligenceMember::getName).toList());
    assertTrue(snapshot.getGlobals().stream().filter(item -> item.getName().equals("empty")).findFirst().orElseThrow().getShape().isUnknown());
    assertEquals(List.of("test.Tools"), snapshot.getJavaImports());
    assertEquals(snapshot.getVersionToken(), factory.build("javascript", "graaljs", "helper").getVersionToken());
    assertNotEquals(snapshot.getVersionToken(), factory.build("groovy", "groovy", "helper").getVersionToken());
    javaType.setSimpleName("Renamed");
    assertNotEquals(snapshot.getVersionToken(), factory.build("javascript", "graaljs", "helper").getVersionToken());
  }

  @Test void unavailableCatalogProducesEmptyEnvironment() {
    var snapshot = new EnvironmentSnapshotFactory(() -> null, null).build("spel", "spel", "helper");
    assertTrue(snapshot.getGlobals().isEmpty());
    assertTrue(snapshot.getJavaTypes().isEmpty());
    assertTrue(snapshot.getJavaImports().isEmpty());
  }

  @Test void indexedDestinationPathsPreserveSiblingProperties() {
    var shape = TypeShape.hierarchicalObject(new LinkedHashMap<>(Map.of(
        "items[0].name", TypeShape.scalar("string"), "items[0].active", TypeShape.scalar("boolean"))));
    assertEquals("string", shape.property("items").element().property("name").type());
    assertEquals("boolean", shape.property("items").element().property("active").type());
  }
}
