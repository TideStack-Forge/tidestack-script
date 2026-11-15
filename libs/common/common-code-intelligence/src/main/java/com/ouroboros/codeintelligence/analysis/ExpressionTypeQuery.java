package com.ouroboros.codeintelligence.analysis;

import java.util.Map;
import com.ouroboros.codeintelligence.type.TypeShape;

@FunctionalInterface
public interface ExpressionTypeQuery {
  TypeShape infer(String expression, Map<String, TypeShape> bindings);
}
