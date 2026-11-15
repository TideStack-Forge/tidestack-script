package com.ouroboros.script.intelligence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import com.ouroboros.script.intelligence.impl.ClassGraphJavaTypeMetadataIndex;

public class RuntimeScriptIntelligenceSnapshotFactoryTest {

  @Test
  public void build_forJavascript_projectsWrappersAndLanguageHints() {
    var wrappers = new LinkedHashMap<String, Object>();
    wrappers.put("Dates", FakeDates.class);
    wrappers.put("Exceptions", new FakeExceptionsContext());

    var factory = new RuntimeScriptIntelligenceSnapshotFactory(
        () -> wrappers,
        new StubJavaTypeMetadataIndex(Collections.singletonList("java.util.UUID"))
    );

    var snapshot = factory.build("javascript", "dev-helper-runtime");

    assertEquals("logicflow-graaljs", snapshot.getRuntimeProfile());
    assertEquals("dev-helper-runtime", snapshot.getSource());
    assertTrue(snapshot.getJavaImports().contains("java.util.UUID"));
    assertTrue(containsGlobal(snapshot, "Dates", "context-wrapper"));
    assertEquals(
        Boolean.TRUE,
        ((Map<?, ?>) snapshot.getLanguageProjectionHints().get("javascript")).get("excludeBrowserGlobals")
    );
  }

  @Test
  public void load_allowlistedTypes_onlyIndexesConfiguredClasses() {
    var index = new ClassGraphJavaTypeMetadataIndex(Arrays.asList("java.util.UUID", "com.ouroboros.util.Dates"));

    var uuid = index.findBySimpleName("UUID");
    assertTrue(uuid.isPresent());
    assertEquals("java.util.UUID", uuid.get().getQualifiedName());
    assertTrue(containsMember(uuid.get().getStaticMethods(), "randomUUID"));
    assertFalse(index.findByQualifiedName("java.io.File").isPresent());
  }

  private boolean containsGlobal(ScriptIntelligenceSnapshot snapshot, String globalName, String sourceTag) {
    return snapshot.getGlobals().stream()
        .anyMatch(global -> globalName.equals(global.getName()) && sourceTag.equals(global.getSourceTag()));
  }

  private boolean containsMember(List<ScriptIntelligenceMember> members, String memberName) {
    return members.stream().anyMatch(member -> memberName.equals(member.getName()));
  }

  private static class StubJavaTypeMetadataIndex implements JavaTypeMetadataIndex {
    private final List<String> importCandidates;
    private final List<ScriptIntelligenceJavaType> types;

    private StubJavaTypeMetadataIndex(List<String> importCandidates) {
      this.importCandidates = importCandidates;
      this.types = importCandidates.stream()
          .map(this::toType)
          .collect(Collectors.toList());
    }

    @Override
    public List<String> getImportCandidates() {
      return importCandidates;
    }

    @Override
    public List<ScriptIntelligenceJavaType> getTypes() {
      return types;
    }

    @Override
    public Optional<ScriptIntelligenceJavaType> findBySimpleName(String simpleName) {
      return types.stream().filter(type -> simpleName.equals(type.getSimpleName())).findFirst();
    }

    @Override
    public Optional<ScriptIntelligenceJavaType> findByQualifiedName(String qualifiedName) {
      return types.stream().filter(type -> qualifiedName.equals(type.getQualifiedName())).findFirst();
    }

    private ScriptIntelligenceJavaType toType(String importCandidate) {
      var javaType = new ScriptIntelligenceJavaType();
      javaType.setQualifiedName(importCandidate);
      javaType.setSimpleName(importCandidate.substring(importCandidate.lastIndexOf('.') + 1));
      return javaType;
    }
  }

  private static class FakeDates {
  }

  private static class FakeExceptionsContext {
  }
}
