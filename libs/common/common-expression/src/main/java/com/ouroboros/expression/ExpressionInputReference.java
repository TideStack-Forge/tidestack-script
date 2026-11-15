package com.ouroboros.expression;

import java.util.List;

/** An external root read by an expression, with its static path when available. */
public record ExpressionInputReference(String root, List<Object> path, boolean dynamic) {
  public ExpressionInputReference {
    path = List.copyOf(path);
  }
}
