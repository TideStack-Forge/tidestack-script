package com.ouroboros.expression.intelligence;

import java.util.LinkedHashMap;
import java.util.Map;
import com.ouroboros.codeintelligence.runtime.EnvironmentSnapshotFactory;
import com.ouroboros.expression.ExpressionContextContributor;
import com.ouroboros.script.intelligence.ScriptIntelligenceSnapshot;
import com.ouroboros.util.SPI;

/** Uses exactly the expression engine's contributor order, without evaluating expressions. */
public final class ExpressionIntelligenceSnapshotFactory {
  public ScriptIntelligenceSnapshot build(String source) {
    return new EnvironmentSnapshotFactory(this::bindings, null).build("spel", "spel", source);
  }

  private Map<String, Object> bindings() {
    var result = new LinkedHashMap<String, Object>();
    SPI.getCachedReversedServiceStream(ExpressionContextContributor.class)
        .map(ExpressionContextContributor::getBindings).forEach(result::putAll);
    return result;
  }
}
