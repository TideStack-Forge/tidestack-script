package com.ouroboros.codeintelligence.type;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** An immutable JSON Schema snapshot; extension keywords retain runtime representation and provenance. */
public record TypeShape(JsonNode schema) {
  private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

  public TypeShape {
    if (schema == null || !schema.isObject()) {
      throw new IllegalArgumentException("Type shape requires an object JSON Schema");
    }
    schema = schema.deepCopy();
  }

  @Override
  public JsonNode schema() {
    return schema.deepCopy();
  }

  /** JSON Schema supplied by a map-based model, including nested objects and collection items. */
  public static TypeShape mapSchema(JsonNode schema) {
    return new TypeShape(normalizeMapSchema(schema, 0));
  }

  private static JsonNode normalizeMapSchema(JsonNode node, int depth) {
    if (!node.isObject() || depth > 32) return unknown("schema-depth-budget").schema();
    var copy = (ObjectNode) node.deepCopy();
    if (copy.has("properties") || "object".equals(copy.path("type").asText())) {
      copy.put("type", "object").put("x-representation", "map");
      copy.path("properties").fields().forEachRemaining(entry ->
          ((ObjectNode) copy.path("properties")).set(entry.getKey(), normalizeMapSchema(entry.getValue(), depth + 1)));
    }
    if (copy.path("items").isObject()) copy.set("items", normalizeMapSchema(copy.path("items"), depth + 1));
    for (var keyword : List.of("anyOf", "oneOf", "allOf")) {
      if (copy.path(keyword).isArray()) {
        var normalized = JSON.arrayNode();
        copy.path(keyword).forEach(branch -> normalized.add(normalizeMapSchema(branch, depth + 1)));
        copy.set(keyword, normalized);
      }
    }
    return copy;
  }

  public static TypeShape unknown(String reason) {
    return new TypeShape(JSON.objectNode().put("x-unknownReason", reason));
  }

  /** Uses the same destination-path expansion as runtime mappings, including indexed lists. */
  public static TypeShape hierarchicalObject(Map<String, TypeShape> fields) {
    var tree = new LinkedHashMap<String, Object>();
    fields.forEach((name, shape) -> com.ouroboros.util.Maps.Hierarchical.setValue(tree, name, shape));
    return shapeTree(tree);
  }

  private static TypeShape shapeTree(Object value) {
    if (value instanceof TypeShape shape) return shape;
    if (value instanceof Map<?, ?> map) {
      var fields = new LinkedHashMap<String, TypeShape>();
      map.forEach((name, item) -> fields.put(String.valueOf(name), shapeTree(item)));
      return object(fields, "map");
    }
    if (value instanceof List<?> list) {
      var elements = list.stream().filter(java.util.Objects::nonNull).map(TypeShape::shapeTree).reduce(TypeShape::merge);
      return array(elements.orElseGet(() -> unknown("empty-mapped-list")));
    }
    return scalar("null");
  }

  public static TypeShape scalar(String type) {
    return new TypeShape(JSON.objectNode().put("type", type));
  }

  public static TypeShape object(Map<String, TypeShape> properties, String representation) {
    var schema = JSON.objectNode().put("type", "object").put("x-representation", representation);
    var fields = schema.putObject("properties");
    var required = schema.putArray("required");
    properties.forEach((name, shape) -> {
      fields.set(name, shape.schema());
      required.add(name);
    });
    schema.put("additionalProperties", false);
    return new TypeShape(schema);
  }

  public static TypeShape array(TypeShape element) {
    var schema = JSON.objectNode().put("type", "array");
    schema.set("items", element.schema());
    return new TypeShape(schema);
  }

  public static TypeShape literal(Object value) {
    if (value == null) return scalar("null");
    if (value instanceof CharSequence) return scalar("string");
    if (value instanceof Boolean) return scalar("boolean");
    if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return scalar("integer");
    if (value instanceof Number) return scalar("number");
    if (value instanceof Map<?, ?> map) {
      var fields = new LinkedHashMap<String, TypeShape>();
      map.forEach((key, item) -> fields.put(String.valueOf(key), literal(item)));
      return object(fields, "map");
    }
    if (value instanceof List<?> list) {
      return array(list.stream().map(TypeShape::literal).reduce(TypeShape::merge).orElseGet(() -> unknown("empty-collection")));
    }
    return unknown("unsupported-literal");
  }

  public String type() {
    return schema.path("type").asText("any");
  }

  public String representation() {
    return schema.path("x-representation").asText("");
  }

  public String javaType() {
    return schema.path("x-javaType").asText("");
  }

  @JsonIgnore
  public boolean isUnknown() {
    return schema.has("x-unknownReason") || schema.isEmpty();
  }

  public Map<String, TypeShape> properties() {
    var fields = new LinkedHashMap<String, TypeShape>();
    schema.path("properties").fields().forEachRemaining(entry -> fields.put(entry.getKey(), new TypeShape(entry.getValue())));
    return java.util.Collections.unmodifiableMap(fields);
  }

  public Set<String> required() {
    var names = new LinkedHashSet<String>();
    schema.path("required").forEach(name -> names.add(name.asText()));
    return java.util.Collections.unmodifiableSet(names);
  }

  public TypeShape property(String name) {
    var field = schema.path("properties").get(name);
    if (field != null && field.isObject()) return new TypeShape(field);
    var alternatives = schema.has("anyOf") ? schema.path("anyOf") : schema.path("oneOf");
    if (alternatives.isArray()) {
      return java.util.stream.StreamSupport.stream(alternatives.spliterator(), false)
          .filter(branch -> !"null".equals(branch.path("type").asText()))
          .map(TypeShape::new).map(shape -> shape.property(name)).reduce(TypeShape::merge)
          .orElseGet(() -> unknown("missing-property:" + name));
    }
    var additional = schema.path("additionalProperties");
    return additional.isObject() ? new TypeShape(additional) : unknown("missing-property:" + name);
  }

  public TypeShape element() {
    var items = schema.path("items");
    return items.isObject() ? new TypeShape(items) : unknown("unknown-element");
  }

  public TypeShape annotated(String key, String value) {
    var copy = (ObjectNode) schema.deepCopy();
    copy.put(key, value);
    return new TypeShape(copy);
  }

  public TypeShape merge(TypeShape other) {
    if (equals(other)) return this;
    if (isUnknown() || other.isUnknown()) return unknown("unknown-alternative");
    if ("object".equals(type()) && "object".equals(other.type()) && representation().equals(other.representation())) {
      return mergeObjects(other);
    }
    if ("array".equals(type()) && "array".equals(other.type())) return array(element().merge(other.element()));
    var union = JSON.objectNode();
    var alternatives = union.putArray("anyOf");
    for (var shape : List.of(this, other)) {
      var branches = shape.schema.path("anyOf");
      if (branches.isArray()) branches.forEach(branch -> { if (!contains(alternatives, branch)) alternatives.add(branch); });
      else if (!contains(alternatives, shape.schema)) alternatives.add(shape.schema.deepCopy());
    }
    return new TypeShape(union);
  }

  private TypeShape mergeObjects(TypeShape other) {
    var fields = new LinkedHashMap<>(properties());
    other.properties().forEach((name, shape) -> fields.merge(name, shape, TypeShape::merge));
    var merged = (ObjectNode) object(fields, representation()).schema();
    var common = new LinkedHashSet<>(required());
    common.retainAll(other.required());
    var required = merged.putArray("required");
    common.forEach(required::add);
    merged.put("additionalProperties", schema.path("additionalProperties").asBoolean(true)
        || other.schema.path("additionalProperties").asBoolean(true));
    return new TypeShape(merged);
  }

  private static boolean contains(JsonNode array, JsonNode candidate) {
    for (var item : array) if (item.equals(candidate)) return true;
    return false;
  }
}
