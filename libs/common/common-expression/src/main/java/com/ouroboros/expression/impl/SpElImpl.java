package com.ouroboros.expression.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import org.springframework.expression.*;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.SpelNode;

import io.vavr.control.Either;
import io.vavr.control.Try;

import com.ouroboros.expression.ContextWrappingHelper;
import com.ouroboros.expression.EL;
import com.ouroboros.expression.ExpressionContextContributor;
import com.ouroboros.expression.ExpressionPlan;
import com.ouroboros.expression.ExpressionInputReference;
import com.ouroboros.util.SPI;

/**
 * @author liansz
 */
public class SpElImpl extends EL {
  private static final ExpressionParser expressionParser = new SpelExpressionParser();
  private static final TemplateParserContext templateParserContext = new TemplateParserContext("${", "}");
  private static final PropertyAccessor nullReturningPropertyAccessor = new NullReturningPropertyAccessor();
  private static final String ESCAPE_PLACEHOLDER = "__ESCAPED_DOLLAR__";

  /**
   * 表达式解析结果缓存。
   * <p>SpEL 自带的表达式缓存只在 {@code parseExpression(String)} 单参重载中生效，
   * 这里带 {@link TemplateParserContext} 调用会绕过该缓存，导致每次求值都重新做词法分析与 AST 构造。
   * 字段类型模板的表达式集合有限且稳定，因此按表达式文本缓存解析结果。
   * <p>解析产物 {@link Expression} 无状态、可并发求值，故直接使用无锁的 ConcurrentHashMap。
   */
  private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

  private static final java.util.Set<String> PREDICATE_NODES = java.util.Set.of(
      "CompoundExpression", "PropertyOrFieldReference", "Indexer", "StringLiteral", "IntLiteral", "LongLiteral",
      "RealLiteral", "BooleanLiteral", "NullLiteral", "OpEQ", "OpNE", "OpGT", "OpGE", "OpLT", "OpLE",
      "OpAnd", "OpOr", "OperatorNot", "Ternary", "Elvis", "OpPlus", "OpMinus", "OpMultiply", "OpDivide",
      "OpModulus", "InlineList", "InlineMap");

  @Override
  public Either<Throwable, Boolean> validatePredicateExpression(String expression) {
    return Try.of(() -> {
      parsePredicate(expression);
      return true;
    }).toEither();
  }

  @Override
  public Either<Throwable, Boolean> evaluatePredicate(String expression, Map<String, Object> values) {
    return Try.of(() -> {
      var parsed = parsePredicate(expression);
      var context = SimpleEvaluationContext.forPropertyAccessors(lenientMapAccessor).build();
      Object result = parsed.getValue(context, values);
      if (!(result instanceof Boolean accepted)) {
        throw new IllegalArgumentException("Restricted predicate must return a boolean");
      }
      return accepted;
    }).toEither();
  }

  @Override
  public Either<Throwable, com.ouroboros.expression.PredicatePlan> compilePredicateExpression(
      String expression, java.util.Set<String> roots) {
    return Try.<com.ouroboros.expression.PredicatePlan>of(() -> {
      var parsed = parsePredicate(expression);
      var paths = new java.util.ArrayList<java.util.List<String>>();
      collectPredicateReads(parsed.getAST(), roots, paths);
      requireBooleanResult(parsed.getAST());
      var immutablePaths = java.util.List.copyOf(paths);
      return new com.ouroboros.expression.PredicatePlan() {
        public java.util.List<java.util.List<String>> accessPaths() { return immutablePaths; }
        public Either<Throwable, Boolean> evaluate(Map<String, Object> values) {
          return Try.of(() -> {
            var context = SimpleEvaluationContext.forPropertyAccessors(lenientMapAccessor).build();
            Object result = parsed.getValue(context, values);
            if (!(result instanceof Boolean accepted)) {
              throw new IllegalArgumentException("Restricted predicate must return a boolean");
            }
            return accepted;
          }).toEither();
        }
      };
    }).toEither();
  }

  private static void collectPredicateReads(SpelNode node, java.util.Set<String> roots,
                                            java.util.List<java.util.List<String>> paths) {
    if (node instanceof org.springframework.expression.spel.ast.CompoundExpression) {
      var path = new java.util.ArrayList<String>();
      for (int index = 0; index < node.getChildCount(); index++) {
        SpelNode segment = node.getChild(index);
        if (segment instanceof org.springframework.expression.spel.ast.PropertyOrFieldReference property) {
          path.add(property.getName());
        } else if (segment instanceof org.springframework.expression.spel.ast.Indexer && segment.getChildCount() == 1
            && segment.getChild(0) instanceof org.springframework.expression.spel.ast.Literal literal
            && (literal.getLiteralValue().getValue() instanceof String || literal.getLiteralValue().getValue() instanceof Integer)) {
          path.add(String.valueOf(literal.getLiteralValue().getValue()));
        } else {
          throw new IllegalArgumentException("Restricted predicate paths require literal property or index segments");
        }
      }
      requirePredicateRoot(path.get(0), roots);
      paths.add(java.util.List.copyOf(path));
      return;
    }
    if (node instanceof org.springframework.expression.spel.ast.PropertyOrFieldReference property) {
      requirePredicateRoot(property.getName(), roots);
      paths.add(java.util.List.of(property.getName()));
      return;
    }
    if (node instanceof org.springframework.expression.spel.ast.Indexer) {
      throw new IllegalArgumentException("Restricted predicate paths require a named root");
    }
    for (int index = 0; index < node.getChildCount(); index++) {
      collectPredicateReads(node.getChild(index), roots, paths);
    }
  }

  private static void requireBooleanResult(SpelNode node) {
    String kind = node.getClass().getSimpleName();
    if (java.util.Set.of("BooleanLiteral", "OpEQ", "OpNE", "OpGT", "OpGE", "OpLT", "OpLE", "OpAnd", "OpOr",
        "OperatorNot", "CompoundExpression", "PropertyOrFieldReference").contains(kind)) { return; }
    if (kind.equals("Ternary")) {
      requireBooleanResult(node.getChild(1));
      requireBooleanResult(node.getChild(2));
      return;
    }
    if (kind.equals("Elvis")) {
      requireBooleanResult(node.getChild(0));
      requireBooleanResult(node.getChild(1));
      return;
    }
    throw new IllegalArgumentException("Restricted predicate must return a boolean");
  }

  private static void requirePredicateRoot(String name, java.util.Set<String> roots) {
    if (!roots.contains(name)) {
      throw new IllegalArgumentException("Restricted predicate root is unavailable: " + name);
    }
  }

  private static SpelExpression parsePredicate(String expression) {
    if (expression == null || !expression.startsWith("${") || !expression.endsWith("}")) {
      throw new IllegalArgumentException("Restricted predicate must be a single expression");
    }
    var parsed = (SpelExpression) expressionParser.parseExpression(expression.substring(2, expression.length() - 1));
    validatePredicateNode(parsed.getAST());
    return parsed;
  }

  private static void validatePredicateNode(SpelNode node) {
    if (!PREDICATE_NODES.contains(node.getClass().getSimpleName())) {
      throw new IllegalArgumentException("Restricted predicate does not allow " + node.getClass().getSimpleName());
    }
    for (int index = 0; index < node.getChildCount(); index++) {
      validatePredicateNode(node.getChild(index));
    }
  }

  private final Optional<Map<String, Object>> contributedBindings = SPI.getReversedServiceStream(ExpressionContextContributor.class)
      .map(ExpressionContextContributor::getBindings)
      .reduce(ContextWrappingHelper::new);

  private static Expression parseCached(String expression) {
    return EXPRESSION_CACHE.computeIfAbsent(expression,
        key -> expressionParser.parseExpression(key, templateParserContext));
  }

  @Override
  public Either<Throwable, Boolean> validateExpressionSyntax(String expression) {
    return Try.of(() -> {
      // Preserve UTF-16 offsets for editor diagnostics while retaining the runtime escape semantics.
      expressionParser.parseExpression(expression.replace("$${", "__{"), templateParserContext);
      return true;
    }).toEither();
  }

  @Override
  public Either<Throwable, ExpressionPlan> compileExpression(String expression) {
    return Try.<ExpressionPlan>of(() -> {
      String preprocessed = expression.replace("$${", ESCAPE_PLACEHOLDER + "{");
      Expression parsed = parseCached(preprocessed);
      Optional<java.util.List<Object>> path = parsed instanceof SpelExpression spel
          ? selectorPath(spel.getAST()) : Optional.empty();
      return new ExpressionPlan() {
        @Override
        public Optional<java.util.List<Object>> selectorPath() { return path; }

        @Override
        public Either<Throwable, Boolean> isPresent(Map<String, Object> values) {
          return Try.of(() -> {
            if (path.isEmpty()) { return true; }
            var contextMap = wrapContext(values);
            var single = SINGLE_PLACEHOLDER.matcher(preprocessed);
            if (single.matches() && contextMap.containsKey(single.group(1))) { return true; }
            return selectorPresent(path.get(), contextMap);
          }).toEither();
        }

        @Override
        public Either<Throwable, java.util.List<ExpressionInputReference>> inputReferences() {
          return Try.of(() -> {
            var references = new java.util.LinkedHashSet<ExpressionInputReference>();
            collectExpressionInputs(parsed, references);
            return java.util.List.copyOf(references);
          }).toEither();
        }

        @Override
        public Either<Throwable, Object> evaluate(Map<String, Object> values) {
          return apply(expression, values);
        }
      };
    }).toEither();
  }

  private void collectExpressionInputs(Expression expression, java.util.Set<ExpressionInputReference> references) {
    if (expression instanceof SpelExpression spel) {
      collectInputReads(spel.getAST(), references);
    } else if (expression instanceof org.springframework.expression.common.CompositeStringExpression composite) {
      for (var part : composite.getExpressions()) {
        collectExpressionInputs(part, references);
      }
    }
  }

  private void collectInputReads(SpelNode node, java.util.Set<ExpressionInputReference> references) {
    if (node instanceof org.springframework.expression.spel.ast.Projection
        || node instanceof org.springframework.expression.spel.ast.Selection
        || node instanceof org.springframework.expression.spel.ast.VariableReference
            && java.util.Set.of("#root", "#this").contains(node.toStringAST())) {
      throw new IllegalArgumentException("Input analysis does not support scoped collection expressions");
    }
    if (node instanceof org.springframework.expression.spel.ast.CompoundExpression) {
      var first = node.getChild(0);
      if (first instanceof org.springframework.expression.spel.ast.PropertyOrFieldReference property) {
        addInputReference(property.getName(), selectorPath(node).orElse(java.util.List.of(property.getName())),
            selectorPath(node).isEmpty(), references);
      } else {
        collectInputReads(first, references);
      }
      // Later property names belong to the receiver. Indexes and method arguments can read roots.
      for (int index = 1; index < node.getChildCount(); index++) {
        var segment = node.getChild(index);
        if (segment instanceof org.springframework.expression.spel.ast.Projection
            || segment instanceof org.springframework.expression.spel.ast.Selection) {
          throw new IllegalArgumentException("Input analysis does not support scoped collection expressions");
        }
        for (int child = 0; child < segment.getChildCount(); child++) {
          collectInputReads(segment.getChild(child), references);
        }
      }
      return;
    }
    if (node instanceof org.springframework.expression.spel.ast.PropertyOrFieldReference property) {
      addInputReference(property.getName(), java.util.List.of(property.getName()), false, references);
      return;
    }
    if (node instanceof org.springframework.expression.spel.ast.VariableReference) {
      var name = node.toStringAST().substring(1);
      if (!"this".equals(name) && !"root".equals(name)) {
        addInputReference(name, java.util.List.of(name), false, references);
      }
      return;
    }
    for (int index = 0; index < node.getChildCount(); index++) {
      if (node instanceof org.springframework.expression.spel.ast.InlineMap && index % 2 == 0) {
        continue;
      }
      collectInputReads(node.getChild(index), references);
    }
  }

  private void addInputReference(String root, java.util.List<Object> path, boolean dynamic,
                                 java.util.Set<ExpressionInputReference> references) {
    if (contributedBindings.map(bindings -> bindings.containsKey(root)).orElse(false)) {
      return;
    }
    references.add(new ExpressionInputReference(root, path, dynamic));
  }

  private static Optional<java.util.List<Object>> selectorPath(SpelNode node) {
    if (node instanceof org.springframework.expression.spel.ast.PropertyOrFieldReference property) {
      return Optional.of(java.util.List.of(property.getName()));
    }
    if (!(node instanceof org.springframework.expression.spel.ast.CompoundExpression)) {
      return Optional.empty();
    }
    var path = new java.util.ArrayList<Object>();
    for (int index = 0; index < node.getChildCount(); index++) {
      SpelNode segment = node.getChild(index);
      if (segment instanceof org.springframework.expression.spel.ast.PropertyOrFieldReference property) {
        path.add(property.getName());
      } else if (segment instanceof org.springframework.expression.spel.ast.Indexer && segment.getChildCount() == 1
          && segment.getChild(0) instanceof org.springframework.expression.spel.ast.Literal literal
          && (literal.getLiteralValue().getValue() instanceof String || literal.getLiteralValue().getValue() instanceof Integer)) {
        path.add(literal.getLiteralValue().getValue());
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(java.util.List.copyOf(path));
  }

  private static boolean selectorPresent(java.util.List<Object> path, Map<String, Object> values) throws AccessException {
    Object parent = values;
    var accessor = new org.springframework.expression.spel.support.ReflectivePropertyAccessor();
    var context = new StandardEvaluationContext(values);
    for (int index = 0; index < path.size(); index++) {
      Object key = path.get(index);
      boolean last = index == path.size() - 1;
      if (parent instanceof Map<?, ?> map) {
        if (!map.containsKey(key)) { return false; }
        if (!last) { parent = map.get(key); }
      } else if (parent instanceof java.util.List<?> list && key instanceof Integer offset) {
        if (offset < 0 || offset >= list.size()) { return false; }
        if (!last) { parent = list.get(offset); }
      } else if (parent != null && parent.getClass().isArray() && key instanceof Integer offset) {
        if (offset < 0 || offset >= java.lang.reflect.Array.getLength(parent)) { return false; }
        if (!last) { parent = java.lang.reflect.Array.get(parent, offset); }
      } else if (parent != null && key instanceof String name && accessor.canRead(context, parent, name)) {
        if (!last) { parent = accessor.read(context, parent, name).getValue(); }
      } else {
        return false;
      }
    }
    return true;
  }

  private Map<String, Object> wrapContext(Map<String, Object> variables) {
    return contributedBindings.<Map<String, Object>>map(w -> new ContextWrappingHelper(variables, w))
        .orElse(variables == null ? java.util.Collections.emptyMap() : variables);
  }

  /**
   * 匹配形如 {@code ${key}} 的单一占位符（键允许字母、数字、下划线、点、连字符）。
   */
  private static final java.util.regex.Pattern SINGLE_PLACEHOLDER =
      java.util.regex.Pattern.compile("^\\$\\{\\s*([\\w.$\\-]+)\\s*}$");

  @Override
  public Either<Throwable, Object> apply(String expressionStr, Map<String, Object> variables) {
    var expressionResult = Try.of(() -> {
      // 预处理：将 $${ 替换为临时占位符，避免被 SpEL 解析
      String preprocessed = expressionStr.replace("$${", ESCAPE_PLACEHOLDER + "{");

      var contextMap = wrapContext(variables);

      // 简单占位符 ${key} 直接取映射值：这类表达式占绝大多数，
      // 走 SpEL 会为每个键构造求值上下文并依赖属性访问器链，既慢又容易被访问器语义影响。
      var single = SINGLE_PLACEHOLDER.matcher(preprocessed);
      if (single.matches()) {
        var key = single.group(1);
        if (contextMap != null && contextMap.containsKey(key)) {
          return contextMap.get(key);
        }
        // 字面量（${0} / ${1.5} / ${true} / ${null}）不是变量名，必须交给 SpEL 求值：
        // 若在这里按变量名短路返回 null，${ 0 } 形式的默认值就会丢失。
        if (!isLiteralToken(key)) {
          // 点号键可能是嵌套属性路径（例如 ${user.name}），直接命中失败时交给 SpEL 解析；
          // 简单键维持原有“缺失即 null”的语义。
          if (!key.contains(".")) {
            return null;
          }
        }
      }

      StandardEvaluationContext context = new StandardEvaluationContext(contextMap);
      context.addPropertyAccessor(lenientMapAccessor);
      context.addPropertyAccessor(nullReturningPropertyAccessor);
      var expression = parseCached(preprocessed);
      Object result = expression.getValue(context);

      // 后处理：将临时占位符还原为 ${
      if (result instanceof String) {
        result = ((String) result).replace(ESCAPE_PLACEHOLDER, "$");
      }
      
      return result;
    });
    return expressionResult.toEither();
  }

  /**
   * 判断单占位符中的记号是字面量还是变量名。
   * <p>变量名不会以数字、正负号或点号开头（{@code ${0}}、{@code ${-1}}、{@code ${1.5}} 都是字面量），
   * 也不会是 {@code true} / {@code false} / {@code null} 这类 SpEL 关键字。
   */
  private static boolean isLiteralToken(String token) {
    if (token.isEmpty()) {
      return false;
    }
    var first = token.charAt(0);
    if (Character.isDigit(first) || first == '+' || first == '-' || first == '.') {
      return true;
    }
    return "true".equalsIgnoreCase(token) || "false".equalsIgnoreCase(token) || "null".equalsIgnoreCase(token);
  }

  /**
   * Map 取值访问器：键缺失时返回 null，而不是像 Spring 的 {@code MapAccessor} 那样抛出异常。
   * <p>必须用本访问器替换 {@code MapAccessor}：后者在键缺失时抛
   * {@code MapAccessException}，SpEL 的 {@code PropertyOrFieldReference} 会因此回退到
   * {@link NullReturningPropertyAccessor}，并把“该属性由兜底访问器处理”的结论
   * <b>缓存进对应的 AST 节点</b>。此后即使该键存在，同一表达式实例也永远取到 null。
   * <p>典型表现：先请求一次不带 {@code orderBy} 的列表（键缺失，触发缓存），
   * 之后所有带 {@code orderBy} 的排序请求都会被忽略并回退到默认排序。
   * <p>返回 null 而非抛异常可以让 SpEL 在本次求值中就得到正确结果，兜底访问器不再被选中。
   */
  private static final PropertyAccessor lenientMapAccessor = new PropertyAccessor() {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return new Class<?>[]{Map.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) {
      return target instanceof Map;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) {
      return new TypedValue(((Map<?, ?>) target).get(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) {
      return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) {
      // 表达式上下文只读
    }
  };

  private static class NullReturningPropertyAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return null;
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
      return true;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
      return new TypedValue(null);
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
      return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
    }
  }
}
