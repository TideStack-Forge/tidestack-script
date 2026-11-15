package com.ouroboros.codeintelligence.type;

import java.beans.Introspector;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Reads declared signatures only; never reads fields or invokes application methods. */
public final class JavaShapeIntrospector {
  public TypeShape describe(Type type) {
    return describe(type, Map.of(), new HashSet<>(), 0, new int[] { 2048 });
  }

  private TypeShape describe(Type type, Map<TypeVariable<?>, Type> inherited, Set<Class<?>> active, int depth, int[] budget) {
    if (--budget[0] < 0) return TypeShape.unknown("java-type-size");
    if (depth > 6) return TypeShape.unknown("java-type-depth");
    type = resolve(type, inherited);
    if (type instanceof WildcardType wildcard) return describe(wildcard.getUpperBounds()[0], inherited, active, depth + 1, budget);
    if (type instanceof TypeVariable<?>) return TypeShape.unknown("erased-java-type");
    if (type instanceof GenericArrayType array) return TypeShape.array(describe(array.getGenericComponentType(), inherited, active, depth + 1, budget));
    var bindings = new HashMap<>(inherited);
    Class<?> raw;
    if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> clazz) {
      raw = clazz;
      var variables = raw.getTypeParameters();
      var arguments = parameterized.getActualTypeArguments();
      for (int i = 0; i < variables.length; i++) bindings.put(variables[i], resolve(arguments[i], inherited));
    } else if (type instanceof Class<?> clazz) raw = clazz;
    else return TypeShape.unknown("unsupported-java-type");
    if (raw == Object.class) return TypeShape.unknown("java-object");
    if (raw == void.class || raw == Void.class) return TypeShape.scalar("null");
    if (raw == boolean.class || raw == Boolean.class) return TypeShape.scalar("boolean");
    if (raw == char.class || raw == Character.class || CharSequence.class.isAssignableFrom(raw) || raw.isEnum()) return TypeShape.scalar("string");
    if (raw.isPrimitive() || Number.class.isAssignableFrom(raw)) return TypeShape.scalar("number");
    if (raw.getName().startsWith("java.time.") || java.util.Date.class.isAssignableFrom(raw) || raw == java.util.UUID.class) return TypeShape.scalar("string").annotated("x-javaType", raw.getName());
    if (raw.isArray()) return TypeShape.array(describe(raw.getComponentType(), bindings, active, depth + 1, budget));
    collectBindings(raw, bindings, new HashSet<>());
    if (Collection.class.isAssignableFrom(raw)) {
      return TypeShape.array(describe(resolve(Collection.class.getTypeParameters()[0], bindings), bindings, active, depth + 1, budget));
    }
    if (Map.class.isAssignableFrom(raw)) {
      var schema = (com.fasterxml.jackson.databind.node.ObjectNode) TypeShape.object(Map.of(), "map").schema();
      schema.set("additionalProperties", describe(resolve(Map.class.getTypeParameters()[1], bindings), bindings, active, depth + 1, budget).schema());
      return new TypeShape(schema).annotated("x-javaType", raw.getName());
    }
    if (!active.add(raw)) return TypeShape.unknown("recursive-java-type").annotated("x-javaType", raw.getName());
    try {
      var properties = new LinkedHashMap<String, TypeShape>();
      for (var field : raw.getFields()) {
        if (!Modifier.isStatic(field.getModifiers())) properties.put(field.getName(), describe(field.getGenericType(), bindings, active, depth + 1, budget));
      }
      for (var method : raw.getMethods()) {
        if (Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()
            || method.getParameterCount() != 0 || method.getDeclaringClass() == Object.class) continue;
        var name = method.getName();
        String property = null;
        if (name.startsWith("get") && name.length() > 3 && method.getReturnType() != void.class) property = Introspector.decapitalize(name.substring(3));
        else if (name.startsWith("is") && name.length() > 2 && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) property = Introspector.decapitalize(name.substring(2));
        if (property != null) properties.put(property, describe(method.getGenericReturnType(), bindings, active, depth + 1, budget));
      }
      if (raw.isRecord()) {
        for (var component : raw.getRecordComponents()) properties.put(component.getName(), describe(component.getGenericType(), bindings, active, depth + 1, budget));
      }
      var schema = (com.fasterxml.jackson.databind.node.ObjectNode) TypeShape.object(properties, "java").annotated("x-javaType", raw.getName()).schema();
      var methods = schema.putArray("x-javaMethods");
      for (var method : raw.getMethods()) {
        if (Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()
            || method.getDeclaringClass() == Object.class) continue;
        var descriptor = methods.addObject().put("name", method.getName()).put("memberKind", "method")
            .put("returnTypeDisplayName", method.getReturnType().getSimpleName());
        var parameters = descriptor.putArray("parameterTypeDisplayNames");
        for (var parameter : method.getParameterTypes()) parameters.add(parameter.getSimpleName());
        descriptor.set("shape", com.ouroboros.util.Json.toBean(com.fasterxml.jackson.databind.JsonNode.class,
            com.ouroboros.util.Json.toJsonString(describe(method.getGenericReturnType(), bindings, active, depth + 1, budget))));
      }
      return new TypeShape(schema);
    } finally {
      active.remove(raw);
    }
  }

  private static Type resolve(Type type, Map<TypeVariable<?>, Type> bindings) {
    var visited = new HashSet<Type>();
    while (type instanceof TypeVariable<?> variable && bindings.containsKey(variable) && visited.add(type)) type = bindings.get(variable);
    return type;
  }

  private static void collectBindings(Class<?> type, Map<TypeVariable<?>, Type> bindings, Set<Class<?>> visited) {
    if (type == null || !visited.add(type)) return;
    var parents = new java.util.ArrayList<Type>(java.util.Arrays.asList(type.getGenericInterfaces()));
    if (type.getGenericSuperclass() != null) parents.add(type.getGenericSuperclass());
    for (var parent : parents) {
      if (parent instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) {
        var variables = raw.getTypeParameters();
        var arguments = parameterized.getActualTypeArguments();
        for (int i = 0; i < variables.length; i++) bindings.put(variables[i], resolve(arguments[i], bindings));
        collectBindings(raw, bindings, visited);
      } else if (parent instanceof Class<?> raw) collectBindings(raw, bindings, visited);
    }
  }
}
