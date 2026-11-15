package com.ouroboros.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import com.ouroboros.util.Dates;

public class TestJs {
  AbstractScriptEngine engine = new JavaScriptEngine();

  @Test
  public void testMapAccess() {
    var bindingsMap = new HashMap<String, Object>() {{
      put("$1", Collections.singletonMap("abc", Collections.singletonMap("def", 1)));
    }};

    var result = engine.execute("return $1.abc.def", bindingsMap);
    assertTrue(result.isSuccess());
  }


  @Test
  public void testListAccess() {
    var list = Arrays.asList(1, 2, 3, 4);
    var bindingsMap = new HashMap<String, Object>() {{
      put("$1", list);
    }};

    var result = engine.execute("return Java.from($1).reduce((a, b) => a + b)", bindingsMap);
    var result2 = engine.execute("return Java.from($1).map(a => a * 2)", bindingsMap);
    assertTrue(result.isSuccess());
    assertTrue(result2.isSuccess());
  }

  @Test
  public void testCompile() {
    var bindingsMap = new HashMap<String, Object>() {{
      put("$1", Collections.singletonMap("abc", Collections.singletonMap("def", 1)));
    }};
    var compiled = engine.compile("return $1.abc.def+1");
    var result = compiled.apply(bindingsMap);
    assertTrue(result.isSuccess());
    assertEquals(2, result.get());
  }

  @Test
  public void testDates() {
    var bindingMap = new HashMap<String, Object>() {{
      put("abc", 1);
    }};
    var js = """
        return Dates.now();
        """;
    var result = engine.execute(js, bindingMap);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testFunction() {
    var bindingMap = new HashMap<String, Object>() {{
      put("hello", ((Function<String, String>) (String str) -> "Hello, " + str));
    }};
    var result = engine.execute("return hello('world')", bindingMap);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testStaticMethod() {
    var result = engine.execute("return Dates.now()", null);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testJsObjectAndMap() {
    var bindingMap = new HashMap<String, Object>() {{
      put("abc", ((Function<Map<String, Object>, Integer>) map -> map.size()));
    }};
    var result = engine.execute("return abc({ abc: 1, def: 2 })", bindingMap);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testJsToJava() {
    var bindingMap = new HashMap<String, Object>() {{
      put("abc", ((Function<String, Integer>) str -> str.length()));
      put("def", Dates.now());
    }};
    var result = engine.execute("return Maps.pick({a:1,b:2}, 'a').a", bindingMap);
    var result2 = engine.execute("return Dates.toString(new Date(), 'yyyy')", bindingMap);
    var result3 = engine.execute("return Dates.toLocalDateTime(new Date().getTime())", bindingMap);
    assertTrue(result.isSuccess());
    assertTrue(result2.isSuccess());
    assertTrue(result3.isSuccess());
  }
}
