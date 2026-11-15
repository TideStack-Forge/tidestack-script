package com.ouroboros.expression;

import java.util.List;
import java.util.Map;

import io.vavr.control.Either;

/** An immutable restricted expression and all of its statically declared value reads. */
public interface PredicatePlan {
  List<List<String>> accessPaths();

  Either<Throwable, Boolean> evaluate(Map<String, Object> values);
}
