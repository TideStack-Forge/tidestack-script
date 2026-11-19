package com.ouroboros.script.intelligence;

import java.util.ArrayList;
import java.util.List;

public class ScriptIntelligenceGlobal {
  private CodeCandidateOrigin origin;
  private com.ouroboros.codeintelligence.type.TypeShape shape;
  private String name;
  private String kind;
  private String typeDisplayName;
  private String qualifiedTypeName;
  private String documentation;
  private String sourceTag;
  private List<ScriptIntelligenceMember> members = new ArrayList<>();

  public CodeCandidateOrigin getOrigin() { return origin; }

  public void setOrigin(CodeCandidateOrigin origin) { this.origin = origin; }

  public com.ouroboros.codeintelligence.type.TypeShape getShape() { return shape; }

  public void setShape(com.ouroboros.codeintelligence.type.TypeShape shape) { this.shape = shape; }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getTypeDisplayName() {
    return typeDisplayName;
  }

  public void setTypeDisplayName(String typeDisplayName) {
    this.typeDisplayName = typeDisplayName;
  }

  public String getQualifiedTypeName() {
    return qualifiedTypeName;
  }

  public void setQualifiedTypeName(String qualifiedTypeName) {
    this.qualifiedTypeName = qualifiedTypeName;
  }

  public String getDocumentation() {
    return documentation;
  }

  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public String getSourceTag() {
    return sourceTag;
  }

  public void setSourceTag(String sourceTag) {
    this.sourceTag = sourceTag;
  }

  public List<ScriptIntelligenceMember> getMembers() {
    return members;
  }

  public void setMembers(List<ScriptIntelligenceMember> members) {
    this.members = members;
  }
}
