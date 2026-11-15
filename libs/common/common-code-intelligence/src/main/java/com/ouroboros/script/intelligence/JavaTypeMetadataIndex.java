package com.ouroboros.script.intelligence;

import java.util.List;
import java.util.Optional;

public interface JavaTypeMetadataIndex {
  List<String> getImportCandidates();

  List<ScriptIntelligenceJavaType> getTypes();

  Optional<ScriptIntelligenceJavaType> findBySimpleName(String simpleName);

  Optional<ScriptIntelligenceJavaType> findByQualifiedName(String qualifiedName);
}
