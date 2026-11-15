package com.ouroboros.script.intelligence.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

import com.ouroboros.script.intelligence.JavaTypeMetadataIndex;
import com.ouroboros.script.intelligence.ScriptIntelligenceJavaType;
import com.ouroboros.script.intelligence.ScriptIntelligenceMember;
import com.ouroboros.util.Json;

public class ClassGraphJavaTypeMetadataIndex implements JavaTypeMetadataIndex {
  private static final String DEFAULT_ALLOWLIST_RESOURCE = "META-INF/ouroboros/script-intelligence/java-allowlist.json";

  private final List<String> allowlistEntries;
  private Map<String, ScriptIntelligenceJavaType> typesByQualifiedName = Collections.emptyMap();
  private Map<String, ScriptIntelligenceJavaType> typesBySimpleName = Collections.emptyMap();
  private List<String> importCandidates = Collections.emptyList();

  public ClassGraphJavaTypeMetadataIndex() {
    this(loadDefaultAllowlist());
  }

  public ClassGraphJavaTypeMetadataIndex(List<String> allowlistEntries) {
    this.allowlistEntries = allowlistEntries == null ? Collections.<String>emptyList() : new ArrayList<>(allowlistEntries);
    refresh();
  }

  public final void refresh() {
    var acceptedClasses = allowlistEntries.stream()
        .filter(entry -> entry.contains("."))
        .filter(entry -> Character.isUpperCase(entry.charAt(entry.lastIndexOf('.') + 1)))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    var acceptedPackages = allowlistEntries.stream()
        .filter(entry -> !acceptedClasses.contains(entry))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    var classGraph = new ClassGraph()
        .enableClassInfo()
        .enableFieldInfo()
        .enableMethodInfo()
        .enableSystemJarsAndModules();
    if (!acceptedPackages.isEmpty()) {
      classGraph = classGraph.acceptPackages(acceptedPackages.toArray(new String[0]));
    }
    if (!acceptedClasses.isEmpty()) {
      classGraph = classGraph.acceptClasses(acceptedClasses.toArray(new String[0]));
    }

    try (ScanResult scanResult = classGraph.scan()) {
      var indexedTypes = scanResult.getAllClasses().stream()
          .filter(ClassInfo::isPublic)
          .filter(classInfo -> isAllowlisted(classInfo.getName(), acceptedClasses, acceptedPackages))
          .map(this::toJavaType)
          .sorted(Comparator.comparing(ScriptIntelligenceJavaType::getQualifiedName))
          .collect(Collectors.toList());

      var qualifiedNameMap = new LinkedHashMap<String, ScriptIntelligenceJavaType>();
      var groupedBySimpleName = new LinkedHashMap<String, List<ScriptIntelligenceJavaType>>();
      for (ScriptIntelligenceJavaType indexedType : indexedTypes) {
        qualifiedNameMap.put(indexedType.getQualifiedName(), indexedType);
        groupedBySimpleName.computeIfAbsent(indexedType.getSimpleName(), key -> new ArrayList<>()).add(indexedType);
      }

      var simpleNameMap = new LinkedHashMap<String, ScriptIntelligenceJavaType>();
      for (Map.Entry<String, List<ScriptIntelligenceJavaType>> entry : groupedBySimpleName.entrySet()) {
        if (entry.getValue().size() == 1) {
          simpleNameMap.put(entry.getKey(), entry.getValue().get(0));
        }
      }

      typesByQualifiedName = qualifiedNameMap;
      typesBySimpleName = simpleNameMap;
      importCandidates = new ArrayList<>(qualifiedNameMap.keySet());
    }
  }

  @Override
  public List<String> getImportCandidates() {
    return Collections.unmodifiableList(importCandidates);
  }

  @Override
  public List<ScriptIntelligenceJavaType> getTypes() {
    return Collections.unmodifiableList(new ArrayList<>(typesByQualifiedName.values()));
  }

  @Override
  public Optional<ScriptIntelligenceJavaType> findBySimpleName(String simpleName) {
    return Optional.ofNullable(typesBySimpleName.get(simpleName));
  }

  @Override
  public Optional<ScriptIntelligenceJavaType> findByQualifiedName(String qualifiedName) {
    return Optional.ofNullable(typesByQualifiedName.get(qualifiedName));
  }

  private boolean isAllowlisted(String className, Set<String> acceptedClasses, Set<String> acceptedPackages) {
    if (acceptedClasses.contains(className)) {
      return true;
    }
    for (String acceptedPackage : acceptedPackages) {
      if (className.startsWith(acceptedPackage + ".")) {
        return true;
      }
    }
    return false;
  }

  private ScriptIntelligenceJavaType toJavaType(ClassInfo classInfo) {
    var javaType = new ScriptIntelligenceJavaType();
    javaType.setQualifiedName(classInfo.getName());
    javaType.setSimpleName(classInfo.getSimpleName());
    javaType.setPackageName(classInfo.getPackageName());
    javaType.setConstructors(classInfo.getConstructorInfo().stream()
        .filter(MethodInfo::isPublic)
        .map(this::fromConstructor)
        .sorted(Comparator.comparing(ScriptIntelligenceMember::getName))
        .collect(Collectors.toList()));
    javaType.setStaticMethods(classInfo.getMethodInfo().stream()
        .filter(MethodInfo::isPublic)
        .filter(MethodInfo::isStatic)
        .map(this::fromMethod)
        .sorted(Comparator.comparing(ScriptIntelligenceMember::getName))
        .collect(Collectors.toList()));
    javaType.setInstanceMethods(classInfo.getMethodInfo().stream()
        .filter(MethodInfo::isPublic)
        .filter(methodInfo -> !methodInfo.isConstructor() && !methodInfo.isStatic())
        .map(this::fromMethod)
        .sorted(Comparator.comparing(ScriptIntelligenceMember::getName))
        .collect(Collectors.toList()));
    javaType.setStaticFields(classInfo.getFieldInfo().stream()
        .filter(FieldInfo::isPublic)
        .filter(FieldInfo::isStatic)
        .map(this::fromField)
        .sorted(Comparator.comparing(ScriptIntelligenceMember::getName))
        .collect(Collectors.toList()));
    return javaType;
  }

  private ScriptIntelligenceMember fromConstructor(MethodInfo methodInfo) {
    var member = new ScriptIntelligenceMember();
    member.setName(methodInfo.getClassInfo().getSimpleName());
    member.setMemberKind("constructor");
    member.setDeclaringTypeName(methodInfo.getClassInfo().getName());
    member.setIsStatic(false);
    member.setReturnTypeDisplayName(methodInfo.getClassInfo().getSimpleName());
    member.setParameterTypeDisplayNames(Arrays.stream(methodInfo.getParameterInfo())
        .map(parameterInfo -> toDisplayName(parameterInfo.getTypeDescriptor().toString()))
        .collect(Collectors.toList()));
    return member;
  }

  private ScriptIntelligenceMember fromMethod(MethodInfo methodInfo) {
    var member = new ScriptIntelligenceMember();
    member.setName(methodInfo.getName());
    member.setMemberKind("method");
    member.setDeclaringTypeName(methodInfo.getClassInfo().getName());
    member.setIsStatic(methodInfo.isStatic());
    member.setReturnTypeDisplayName(toDisplayName(methodInfo.getTypeDescriptor().getResultType().toString()));
    member.setParameterTypeDisplayNames(Arrays.stream(methodInfo.getParameterInfo())
        .map(parameterInfo -> toDisplayName(parameterInfo.getTypeDescriptor().toString()))
        .collect(Collectors.toList()));
    return member;
  }

  private ScriptIntelligenceMember fromField(FieldInfo fieldInfo) {
    var member = new ScriptIntelligenceMember();
    member.setName(fieldInfo.getName());
    member.setMemberKind("field");
    member.setDeclaringTypeName(fieldInfo.getClassInfo().getName());
    member.setIsStatic(fieldInfo.isStatic());
    member.setReturnTypeDisplayName(toDisplayName(fieldInfo.getTypeDescriptor().toString()));
    return member;
  }

  private String toDisplayName(String typeDescriptor) {
    if (typeDescriptor == null) {
      return "any";
    }
    if (typeDescriptor.indexOf('.') == -1) {
      return typeDescriptor;
    }
    return typeDescriptor.substring(typeDescriptor.lastIndexOf('.') + 1);
  }

  private static List<String> loadDefaultAllowlist() {
    var entries = new LinkedHashSet<String>();
    try {
      var resources = ClassGraphJavaTypeMetadataIndex.class.getClassLoader().getResources(DEFAULT_ALLOWLIST_RESOURCE);
      while (resources.hasMoreElements()) {
        var resource = resources.nextElement();
        try (InputStream inputStream = resource.openStream()) {
          var config = Json.toBean(JavaAllowlistConfig.class, inputStream);
          if (config != null) {
            if (config.getClasses() != null) entries.addAll(config.getClasses());
            if (config.getPackages() != null) entries.addAll(config.getPackages());
          }
        }
      }
      return List.copyOf(entries);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Cannot load Java intelligence allowlists", exception);
    }
  }

  public static class JavaAllowlistConfig {
    private List<String> classes = new ArrayList<>();
    private List<String> packages = new ArrayList<>();

    public List<String> getClasses() {
      return classes;
    }

    public void setClasses(List<String> classes) {
      this.classes = classes;
    }

    public List<String> getPackages() {
      return packages;
    }

    public void setPackages(List<String> packages) {
      this.packages = packages;
    }
  }
}
