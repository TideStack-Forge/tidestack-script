package com.ouroboros.codeintelligence.runtime;

import com.ouroboros.script.intelligence.*;
import java.util.function.Supplier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;


public class EnvironmentSnapshotFactory {
  private final com.ouroboros.codeintelligence.type.JavaShapeIntrospector shapes = new com.ouroboros.codeintelligence.type.JavaShapeIntrospector();
  private final Supplier<Map<String, Object>> bindings;
  private final JavaTypeMetadataIndex javaTypeMetadataIndex;

  public EnvironmentSnapshotFactory(
      Supplier<Map<String, Object>> bindings,
      JavaTypeMetadataIndex javaTypeMetadataIndex
  ) {
    this.bindings = bindings;
    this.javaTypeMetadataIndex = javaTypeMetadataIndex;
  }

  public ScriptIntelligenceSnapshot build(String language, String runtimeProfile, String source) {

    var snapshot = new ScriptIntelligenceSnapshot();
    snapshot.setRuntimeProfile(runtimeProfile);
    snapshot.setGlobals(buildWrapperGlobals(bindings.get()));
    snapshot.setJavaImports(safeImportCandidates());
    snapshot.setJavaTypes(safeJavaTypes());
    snapshot.setLanguageProjectionHints(buildLanguageProjectionHints(runtimeProfile, language));
    snapshot.setSource(source);
    snapshot.setVersionToken(buildVersionToken(language, snapshot));
    return snapshot;
  }

  private List<ScriptIntelligenceGlobal> buildWrapperGlobals(Map<String, Object> wrappers) {
    if (wrappers == null || wrappers.isEmpty()) {
      return Collections.emptyList();
    }
    return wrappers.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(this::createWrapperGlobal)
        .collect(Collectors.toList());
  }

  private ScriptIntelligenceGlobal createWrapperGlobal(Map.Entry<String, Object> entry) {
    var value = entry.getValue();
    var type = resolveType(value);
    var global = new ScriptIntelligenceGlobal();
    global.setName(entry.getKey());
    global.setKind(value instanceof Class<?> ? "class" : "object");
    global.setTypeDisplayName(toDisplayName(type));
    global.setQualifiedTypeName(type == null ? null : type.getName());
    global.setDocumentation(type == null ? null : type.getName());
    global.setSourceTag("context-wrapper");
    global.setShape(type == null ? com.ouroboros.codeintelligence.type.TypeShape.unknown("null-binding") : shapes.describe(type));
    global.setMembers(type == null ? Collections.<ScriptIntelligenceMember>emptyList() : buildMembers(type, value instanceof Class));
    return global;
  }

  private Class<?> resolveType(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Class) {
      return (Class<?>) value;
    }
    return value.getClass();
  }

  private List<ScriptIntelligenceMember> buildMembers(Class<?> type, boolean staticOnly) {
    var members = new ArrayList<ScriptIntelligenceMember>();
    for (Method method : type.getMethods()) {
      if (method.isSynthetic() || method.isBridge() || Object.class.equals(method.getDeclaringClass())) {
        continue;
      }
      var isStatic = Modifier.isStatic(method.getModifiers());
      if (staticOnly != isStatic) {
        continue;
      }
      members.add(fromMethod(type, method, isStatic));
    }
    for (Field field : type.getFields()) {
      if (Object.class.equals(field.getDeclaringClass())) {
        continue;
      }
      var isStatic = Modifier.isStatic(field.getModifiers());
      if (staticOnly != isStatic) {
        continue;
      }
      members.add(fromField(type, field, isStatic));
    }
    members.sort(Comparator.comparing(ScriptIntelligenceMember::getName));
    return members;
  }

  private ScriptIntelligenceMember fromMethod(Class<?> type, Method method, boolean isStatic) {
    var member = new ScriptIntelligenceMember();
    member.setName(method.getName());
    member.setMemberKind("method");
    member.setDeclaringTypeName(type.getName());
    member.setIsStatic(isStatic);
    member.setReturnTypeDisplayName(toDisplayName(method.getReturnType()));
    member.setShape(shapes.describe(method.getGenericReturnType()));
    member.setParameterTypeDisplayNames(
        java.util.Arrays.stream(method.getParameterTypes())
            .map(this::toDisplayName)
            .collect(Collectors.toList())
    );
    return member;
  }

  private ScriptIntelligenceMember fromField(Class<?> type, Field field, boolean isStatic) {
    var member = new ScriptIntelligenceMember();
    member.setName(field.getName());
    member.setMemberKind("field");
    member.setDeclaringTypeName(type.getName());
    member.setIsStatic(isStatic);
    member.setReturnTypeDisplayName(toDisplayName(field.getType()));
    member.setShape(shapes.describe(field.getGenericType()));
    return member;
  }

  private String toDisplayName(Class<?> type) {
    if (type == null) {
      return "any";
    }
    return StringUtils.defaultIfBlank(type.getSimpleName(), type.getName());
  }

  private Map<String, Object> buildLanguageProjectionHints(String runtimeProfile, String language) {
    var hints = new LinkedHashMap<String, Object>();
    var perLanguageHints = new LinkedHashMap<String, Object>();
    perLanguageHints.put("runtimeProfile", runtimeProfile);
    if ("javascript".equalsIgnoreCase(language)) {
      perLanguageHints.put("excludeBrowserGlobals", true);
    }
    hints.put(StringUtils.defaultIfBlank(language, "unknown"), perLanguageHints);
    return hints;
  }

  private String buildVersionToken(String language, ScriptIntelligenceSnapshot snapshot) {
    var material = new LinkedHashMap<String, Object>();
    material.put("language", language);
    material.put("globals", snapshot.getGlobals());
    material.put("javaImports", new ArrayList<String>(snapshot.getJavaImports()));
    material.put("javaTypes", snapshot.getJavaTypes());
    material.put("source", snapshot.getSource());
    return ContentVersion.of(material);
  }

  private List<String> safeImportCandidates() {
    if (javaTypeMetadataIndex == null || javaTypeMetadataIndex.getImportCandidates() == null) {
      return Collections.emptyList();
    }
    return javaTypeMetadataIndex.getImportCandidates();
  }

  private List<ScriptIntelligenceJavaType> safeJavaTypes() {
    if (javaTypeMetadataIndex == null || javaTypeMetadataIndex.getTypes() == null) {
      return Collections.emptyList();
    }
    return javaTypeMetadataIndex.getTypes();
  }
}
