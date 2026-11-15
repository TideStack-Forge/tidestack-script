package com.ouroboros.expression.intelligence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import com.ouroboros.codeintelligence.analysis.ExpressionTypeQuery;
import com.ouroboros.codeintelligence.type.TypeShape;

/** Signature/AST analysis only. Never creates an EvaluationContext or evaluates an expression. */
public final class SpelTypeQuery implements ExpressionTypeQuery {
  private static final Set<String> BOOLEAN_NODES = Set.of("OpEQ", "OpNE", "OpLT", "OpLE", "OpGT", "OpGE", "OpAnd", "OpOr", "OperatorNot", "OperatorMatches", "OperatorBetween", "OperatorInstanceof");
  private static final Set<String> NUMERIC_NODES = Set.of("OpMinus", "OpMultiply", "OpDivide", "OpModulus", "OperatorPower");
  private final SpelExpressionParser parser = new SpelExpressionParser();

  public TypeShape infer(String expression, Map<String, TypeShape> bindings) {
    if (expression == null || expression.isBlank()) return TypeShape.unknown("empty-expression");
    if (expression.length() > 65536) return TypeShape.unknown("expression-size-budget");
    var source = expression.trim();
    if (source.startsWith("${") && source.endsWith("}")) source = source.substring(2, source.length() - 1);
    if (bindings.containsKey(source)) return bindings.get(source);
    try {
      return inferNode(parser.parseRaw(source).getAST(), bindings, TypeShape.object(bindings, "map"), 0);
    } catch (ParseException exception) {
      return TypeShape.unknown("syntax:" + exception.getPosition());
    }
  }

  private TypeShape inferNode(SpelNode node, Map<String, TypeShape> bindings, TypeShape receiver, int depth) {
    if (depth > 64) return TypeShape.unknown("expression-depth-budget");
    if (node instanceof Literal literal) return TypeShape.literal(literal.getLiteralValue().getValue());
    if (node instanceof PropertyOrFieldReference property) return receiver.property(property.getName());
    if (node instanceof VariableReference variable) return bindings.getOrDefault(variable.toStringAST().substring(1), TypeShape.unknown("unbound-variable"));
    if (node instanceof CompoundExpression) {
      var shape = receiver;
      for (int i = 0; i < node.getChildCount(); i++) shape = inferNode(node.getChild(i), bindings, shape, depth + 1);
      return shape;
    }
    if (node instanceof Indexer) {
      if ("array".equals(receiver.type())) return receiver.element();
      if (node.getChildCount() == 1 && node.getChild(0) instanceof StringLiteral literal) return receiver.property(String.valueOf(literal.getLiteralValue().getValue()));
      return TypeShape.unknown("dynamic-index");
    }
    if (node instanceof MethodReference method) {
      TypeShape result = null;
      for (var signature : receiver.schema().path("x-javaMethods")) {
        if (method.getName().equals(signature.path("name").asText())
            && signature.path("parameterTypeDisplayNames").size() == method.getChildCount()
            && signature.path("shape").path("schema").isObject()) {
          var shape = new TypeShape(signature.path("shape").path("schema"));
          result = result == null ? shape : result.merge(shape);
        }
      }
      return result == null ? TypeShape.unknown("unknown-method-signature:" + method.getName()) : result;
    }
    if (node instanceof Ternary) return inferNode(node.getChild(1), bindings, receiver, depth + 1).merge(inferNode(node.getChild(2), bindings, receiver, depth + 1));
    if (node instanceof Elvis) return inferNode(node.getChild(0), bindings, receiver, depth + 1).merge(inferNode(node.getChild(1), bindings, receiver, depth + 1));
    if (node instanceof InlineList) {
      TypeShape item = null;
      for (int i = 0; i < node.getChildCount(); i++) {
        var next = inferNode(node.getChild(i), bindings, receiver, depth + 1);
        item = item == null ? next : item.merge(next);
      }
      return TypeShape.array(item == null ? TypeShape.unknown("empty-collection") : item);
    }
    if (node instanceof InlineMap) {
      var fields = new LinkedHashMap<String, TypeShape>();
      for (int i = 0; i + 1 < node.getChildCount(); i += 2) {
        var key = node.getChild(i) instanceof StringLiteral literal ? String.valueOf(literal.getLiteralValue().getValue()) : node.getChild(i).toStringAST();
        fields.put(key, inferNode(node.getChild(i + 1), bindings, receiver, depth + 1));
      }
      return TypeShape.object(fields, "map");
    }
    if (BOOLEAN_NODES.contains(node.getClass().getSimpleName())) return TypeShape.scalar("boolean");
    if (NUMERIC_NODES.contains(node.getClass().getSimpleName())) return TypeShape.scalar("number");
    if (node instanceof OpPlus) {
      for (int i = 0; i < node.getChildCount(); i++) if ("string".equals(inferNode(node.getChild(i), bindings, receiver, depth + 1).type())) return TypeShape.scalar("string");
      return TypeShape.scalar("number");
    }
    return TypeShape.unknown("unsupported-ast:" + node.getClass().getSimpleName());
  }
}
