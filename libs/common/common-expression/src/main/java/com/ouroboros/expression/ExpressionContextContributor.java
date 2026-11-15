package com.ouroboros.expression;

import java.util.Map;

public interface ExpressionContextContributor {
  Map<String, Object> getBindings();
}
