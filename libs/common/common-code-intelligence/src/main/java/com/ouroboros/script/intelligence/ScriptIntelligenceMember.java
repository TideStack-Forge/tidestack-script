package com.ouroboros.script.intelligence;

import java.util.ArrayList;
import java.util.List;

public class ScriptIntelligenceMember {
  private com.ouroboros.codeintelligence.type.TypeShape shape;
  private String name;
  private String memberKind;
  private String returnTypeDisplayName;
  private String declaringTypeName;
  private Boolean isStatic;
  private List<String> parameterTypeDisplayNames = new ArrayList<>();

  public com.ouroboros.codeintelligence.type.TypeShape getShape() { return shape; }

  public void setShape(com.ouroboros.codeintelligence.type.TypeShape shape) { this.shape = shape; }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMemberKind() {
    return memberKind;
  }

  public void setMemberKind(String memberKind) {
    this.memberKind = memberKind;
  }

  public String getReturnTypeDisplayName() {
    return returnTypeDisplayName;
  }

  public void setReturnTypeDisplayName(String returnTypeDisplayName) {
    this.returnTypeDisplayName = returnTypeDisplayName;
  }

  public String getDeclaringTypeName() {
    return declaringTypeName;
  }

  public void setDeclaringTypeName(String declaringTypeName) {
    this.declaringTypeName = declaringTypeName;
  }

  public Boolean getIsStatic() {
    return isStatic;
  }

  public void setIsStatic(Boolean isStatic) {
    this.isStatic = isStatic;
  }

  public List<String> getParameterTypeDisplayNames() {
    return parameterTypeDisplayNames;
  }

  public void setParameterTypeDisplayNames(List<String> parameterTypeDisplayNames) {
    this.parameterTypeDisplayNames = parameterTypeDisplayNames;
  }
}
