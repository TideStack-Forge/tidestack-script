package com.ouroboros.codeintelligence.type;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

class TypeShapeTest {
  @Test
  void reflectsInheritedGenericPropertiesWithoutCallingGetters() {
    var shape = new JavaShapeIntrospector().describe(Orders.class);
    assertEquals("string", shape.property("items").element().property("customer").property("name").type());
    assertEquals("java", shape.representation());
    assertEquals("array", shape.property("items").type());
    assertTrue(shape.property("missing").isUnknown());
  }

  @Test
  void rawMapsHaveNoInventedBusinessKeysAndRecursiveTypesAreBounded() {
    var introspector = new JavaShapeIntrospector();
    assertTrue(introspector.describe(Map.class).properties().isEmpty());
    assertTrue(introspector.describe(Link.class).property("next").isUnknown());
  }

  @Test
  void mergesBranchFieldsAsOptionalAndPreservesConflictingTypes() {
    var left = TypeShape.object(Map.of("name", TypeShape.scalar("string")), "map");
    var right = TypeShape.object(Map.of("count", TypeShape.scalar("integer")), "map");
    var merged = left.merge(right);
    assertEquals(2, merged.properties().size());
    assertFalse(merged.required().contains("name"));
    assertEquals(2, TypeShape.scalar("string").merge(TypeShape.scalar("integer")).schema().path("anyOf").size());
  }

  @Test
  void serializesAsJsonSchemaAndDefendsItsSnapshot() throws Exception {
    var shape = TypeShape.object(Map.of("value", TypeShape.scalar("string")), "map");
    var mapper = new ObjectMapper();
    var restored = mapper.readValue(mapper.writeValueAsString(shape), TypeShape.class);
    assertEquals(shape, restored);
    var schema = restored.schema();
    ((com.fasterxml.jackson.databind.node.ObjectNode) schema).remove("properties");
    assertEquals("string", restored.property("value").type());
    assertEquals("array", TypeShape.literal(List.of("$0.name")).type());
  }

  @Test void nullableAndGenericMapPropertiesRemainDescribed() throws Exception {
    var customer = TypeShape.object(Map.of("name", TypeShape.scalar("string")), "map");
    assertEquals("string", customer.merge(TypeShape.scalar("null")).property("name").type());
    var generic = new JavaShapeIntrospector().describe(GenericMap.class.getField("customers").getGenericType());
    assertEquals("string", generic.property("anyKey").property("name").type());
  }

  public static class GenericMap { public Map<String, Customer> customers; }
  public static class Container<T> {
    public List<T> getItems() { throw new AssertionError("getter must not execute"); }
  }
  public static class Orders extends Container<Order> {}
  public record Order(Customer customer) {}
  public record Customer(String name) {}
  public record Link(Link next) {}
}
