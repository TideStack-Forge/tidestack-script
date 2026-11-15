package com.ouroboros.script.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScriptIntelligenceSnapshot {
  private List<Map<String, Object>> diagnostics = new ArrayList<>();
  public List<Map<String, Object>> getDiagnostics() { return diagnostics; }
  public void setDiagnostics(List<Map<String, Object>> diagnostics) { this.diagnostics = List.copyOf(diagnostics); }
  private String runtimeProfile;
  private List<ScriptIntelligenceGlobal> globals = new ArrayList<>();
  private List<String> javaImports = new ArrayList<>();
  private List<ScriptIntelligenceJavaType> javaTypes = new ArrayList<>();
  private Map<String, Object> languageProjectionHints = new LinkedHashMap<>();
  private String versionToken;
  private String source;

  public String getRuntimeProfile() {
    return runtimeProfile;
  }

  public void setRuntimeProfile(String runtimeProfile) {
    this.runtimeProfile = runtimeProfile;
  }

  public List<ScriptIntelligenceGlobal> getGlobals() {
    return globals;
  }

  public void setGlobals(List<ScriptIntelligenceGlobal> globals) {
    this.globals = globals;
  }

  public List<String> getJavaImports() {
    return javaImports;
  }

  public void setJavaImports(List<String> javaImports) {
    this.javaImports = javaImports;
  }

  public List<ScriptIntelligenceJavaType> getJavaTypes() {
    return javaTypes;
  }

  public void setJavaTypes(List<ScriptIntelligenceJavaType> javaTypes) {
    this.javaTypes = javaTypes;
  }

  public Map<String, Object> getLanguageProjectionHints() {
    return languageProjectionHints;
  }

  public void setLanguageProjectionHints(Map<String, Object> languageProjectionHints) {
    this.languageProjectionHints = languageProjectionHints;
  }

  public String getVersionToken() {
    return versionToken;
  }

  public void setVersionToken(String versionToken) {
    this.versionToken = versionToken;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
