package com.ouroboros.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.function.Function;

import org.junit.Test;

public class TestGroovy {
  GroovyEngine engine = new GroovyEngine();

  @Test
  public void test() {
    var bindingsMap = new HashMap<String, Object>() {{
      put("$1", 1);
    }};
    var result = engine.execute("$1+1", bindingsMap);
    assertTrue(result.isSuccess());
    assertEquals(2, result.get());
  }

  @Test
  public void testCompile() {
    var compiled = engine.compile("1+1");
    var result = compiled.apply(null);
    assertTrue(result.isSuccess());
    assertEquals(2, result.get());
  }

  @Test
  public void testDate() {
    var bindingMap = new HashMap<String, Object>() {{
      put("date", LocalDateTime.now());
    }};
    var result = engine.execute("date", bindingMap);
    assertTrue(result.isSuccess());
    assertEquals(bindingMap.get("date"), result.get());
  }

  @Test
  public void testDateUtil() {
    var bindingMap = new HashMap<String, Object>() {{
      put("date", LocalDateTime.now());
    }};

    var result = engine.execute("Dates.now()", bindingMap);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testFunction() {
    var bindingMap = new HashMap<String, Object>() {{
      put("hello", ((Function<String, String>) (String str) -> "Hello, " + str));
    }};
    var result = engine.execute("hello.apply('abc')", bindingMap);
    assertTrue(result.isSuccess());
  }
}
