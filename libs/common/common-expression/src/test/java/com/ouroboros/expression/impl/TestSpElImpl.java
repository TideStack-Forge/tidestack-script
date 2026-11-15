package com.ouroboros.expression.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

public class TestSpElImpl {
  @Test
  public void testEl() {
    String expr = "${ 3 * a }";
    SpElImpl el = new SpElImpl();
    var result = el.apply(expr, Collections.<String, Object>singletonMap("a", 2));
    assertTrue(result.isRight());
  }

  @Test
  public void testNestedMapProperty() {
    Map<String, Object> context = new HashMap<>();
    context.put("user", Collections.<String, Object>singletonMap("name", "张三"));

    var result = new SpElImpl().apply("${user.name}", context);

    assertTrue(result.isRight());
    assertEquals("张三", result.get());
  }

  @Test
  public void testDates() {
    String expr = "${ Dates.now() }";
    SpElImpl el = new SpElImpl();
    var result = el.apply(expr, Collections.<String, Object>singletonMap("a", "2"));
    assertTrue(result.isRight());
  }

  @Test
  public void testEscapedVariable() {
    String expr = "URL: /api/preview?id=$${fileId}";
    SpElImpl el = new SpElImpl();
    var result = el.apply(expr, Collections.emptyMap());
    assertTrue(result.isRight());
    assertEquals("URL: /api/preview?id=${fileId}", result.get());
  }

  @Test
  public void testMultipleEscapedVariables() {
    String expr = "/api/preview?id=$${fileId}&token=$${accessToken}";
    SpElImpl el = new SpElImpl();
    var result = el.apply(expr, Collections.emptyMap());
    assertTrue(result.isRight());
    assertEquals("/api/preview?id=${fileId}&token=${accessToken}", result.get());
  }

  @Test
  public void testMixedExpressionAndEscaped() {
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("baseUrl", "http://example.com");
    
    String expr = "${ baseUrl }/api/preview?id=$${fileId}";
    SpElImpl el = new SpElImpl();
    var result = el.apply(expr, context);
    assertTrue(result.isRight());
    assertEquals("http://example.com/api/preview?id=${fileId}", result.get());
  }

  @Test
  public void testRealWorldUrlTemplate() {
    String expr = "/api/v1/dmp/editor/filetransfer/preview?userFileId=$${fileId}&ot=detail&token=$${accessToken}";
    SpElImpl el = new SpElImpl();
    var result = el.apply(expr, Collections.emptyMap());
    assertTrue(result.isRight());
    assertEquals("/api/v1/dmp/editor/filetransfer/preview?userFileId=${fileId}&ot=detail&token=${accessToken}", result.get());
  }

  @Test
  public void testNullReturningPropertyAccessorReturnsNullAndDisallowsWrite() throws Exception {
    Class<?> accessorClass = Class.forName("com.ouroboros.expression.impl.SpElImpl$NullReturningPropertyAccessor");
    Constructor<?> constructor = accessorClass.getDeclaredConstructor();
    constructor.setAccessible(true);

    PropertyAccessor accessor = (PropertyAccessor) constructor.newInstance();

    assertEquals(null, accessor.getSpecificTargetClasses());
    assertTrue(accessor.canRead(null, new Object(), "missing"));
    assertEquals(null, accessor.read(null, new Object(), "missing").getValue());
    assertTrue(!accessor.canWrite(null, new Object(), "missing"));
    accessor.write(null, new Object(), "missing", new TypedValue("ignored"));
  }
}
