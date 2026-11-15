package com.ouroboros.expression;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.vavr.control.Either;

/** A provider-compiled value expression; invocation values are never retained. */
public interface ExpressionPlan {
  /** Engine-recognized property / fixed-index selector, absent for computed expressions. */
  Optional<List<Object>> selectorPath();

  /** Checks selector membership without reading its final value or evaluating a computation. */
  Either<Throwable, Boolean> isPresent(Map<String, Object> values);

  /** Analysis only: never evaluates the expression or retains invocation values. */
  default Either<Throwable, List<ExpressionInputReference>> inputReferences() {
    return Either.left(new UnsupportedOperationException("Expression provider does not support input analysis"));
  }

  Either<Throwable, Object> evaluate(Map<String, Object> values);
}
