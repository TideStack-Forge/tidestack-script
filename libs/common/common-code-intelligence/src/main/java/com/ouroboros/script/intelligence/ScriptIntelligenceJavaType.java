package com.ouroboros.script.intelligence;

import java.util.ArrayList;
import java.util.List;

public class ScriptIntelligenceJavaType {
  private CodeCandidateOrigin origin;
  private String qualifiedName;
  private String simpleName;
  private String packageName;
  private List<ScriptIntelligenceMember> constructors = new ArrayList<>();
  private List<ScriptIntelligenceMember> staticMethods = new ArrayList<>();
  private List<ScriptIntelligenceMember> staticFields = new ArrayList<>();
  private List<ScriptIntelligenceMember> instanceMethods = new ArrayList<>();

  public CodeCandidateOrigin getOrigin() { return origin; }

  public void setOrigin(CodeCandidateOrigin origin) { this.origin = origin; }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public void setQualifiedName(String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }

  public String getSimpleName() {
    return simpleName;
  }

  public void setSimpleName(String simpleName) {
    this.simpleName = simpleName;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public List<ScriptIntelligenceMember> getConstructors() {
    return constructors;
  }

  public void setConstructors(List<ScriptIntelligenceMember> constructors) {
    this.constructors = constructors;
  }

  public List<ScriptIntelligenceMember> getStaticMethods() {
    return staticMethods;
  }

  public void setStaticMethods(List<ScriptIntelligenceMember> staticMethods) {
    this.staticMethods = staticMethods;
  }

  public List<ScriptIntelligenceMember> getStaticFields() {
    return staticFields;
  }

  public void setStaticFields(List<ScriptIntelligenceMember> staticFields) {
    this.staticFields = staticFields;
  }

  public List<ScriptIntelligenceMember> getInstanceMethods() {
    return instanceMethods;
  }

  public void setInstanceMethods(List<ScriptIntelligenceMember> instanceMethods) {
    this.instanceMethods = instanceMethods;
  }
}
