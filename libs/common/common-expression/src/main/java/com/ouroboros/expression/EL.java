package com.ouroboros.expression;

import java.util.Map;
import java.util.function.BiFunction;

import io.vavr.control.Either;

import com.ouroboros.util.SPI;

@SuppressWarnings("unused")
public abstract class EL implements BiFunction<String, Map<String, Object>, Either<Throwable, Object>> {
  //WARN: 不要删除默认实现，否则这里要加额外判断
  static EL DEFAULT_EL = SPI.getReversedServiceStream(EL.class)
      .findFirst()
      .orElse(null);

  public static Either<Throwable, Object> eval(String expression, Map<String, Object> context) {
    return DEFAULT_EL.apply(expression, context);
  }

  public static Either<Throwable, ExpressionPlan> compile(String expression) {
    return DEFAULT_EL.compileExpression(expression);
  }

  /** Parse editor drafts without evaluating them or retaining them in the runtime expression cache. */
  public static Either<Throwable, Boolean> validateSyntax(String expression) {
    return DEFAULT_EL.validateExpressionSyntax(expression);
  }

  public Either<Throwable, Boolean> validateExpressionSyntax(String expression) {
    return Either.left(new UnsupportedOperationException("Expression provider does not support syntax validation"));
  }

  public Either<Throwable, ExpressionPlan> compileExpression(String expression) {
    return Either.left(new UnsupportedOperationException("Expression provider does not support value plans"));
  }

  public static Either<Throwable, PredicatePlan> compilePredicate(String expression, java.util.Set<String> roots) {
    return DEFAULT_EL.compilePredicateExpression(expression, roots);
  }

  public Either<Throwable, PredicatePlan> compilePredicateExpression(String expression, java.util.Set<String> roots) {
    return Either.left(new UnsupportedOperationException("Expression provider does not support restricted predicate plans"));
  }

  public static Either<Throwable, Boolean> evalPredicate(String expression, Map<String, Object> values) {
    return DEFAULT_EL.evaluatePredicate(expression, values);
  }

  public static Either<Throwable, Boolean> validatePredicate(String expression) {
    return DEFAULT_EL.validatePredicateExpression(expression);
  }

  public Either<Throwable, Boolean> evaluatePredicate(String expression, Map<String, Object> values) {
    return Either.left(new UnsupportedOperationException("Expression provider does not support restricted predicates"));
  }

  public Either<Throwable, Boolean> validatePredicateExpression(String expression) {
    return Either.left(new UnsupportedOperationException("Expression provider does not support restricted predicates"));
  }

  public static <T> Either<Throwable, T> eval(Class<T> clazz, String expression, Map<String, Object> context) {
    return DEFAULT_EL.apply(expression, context)
        .flatMap(obj -> {
          if (clazz.isInstance(obj)) {
            return Either.right(clazz.cast(obj));
          }
          return Either.left(new ClassCastException(String.format("Cannot cast %s to %s", obj.getClass().getName(), clazz.getName())));
        });
  }

  public static String wrapExpression(String expression) {
    if (expression == null || expression.startsWith("${") && expression.endsWith("}")) {
      return expression;
    }
    return String.format("${ %s }", expression);
  }
}
