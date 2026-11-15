package com.ouroboros.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import io.vavr.control.Try;

public class ScriptsTest {

  @Test
  public void executeAndCompile_supportedLanguage_returnsExpectedResult() {
    var executeResult = Scripts.execute("groovy", "1+1", Collections.emptyMap());
    assertTrue(executeResult.isSuccess());
    assertEquals(2, executeResult.get());

    var compiled = Scripts.compile("groovy", "$1+1");
    var bindings = new HashMap<String, Object>();
    bindings.put("$1", 1);
    var compileResult = compiled.apply(bindings);
    assertTrue(compileResult.isSuccess());
    assertEquals(2, compileResult.get());
  }

  @Test
  public void executeAndCompile_unsupportedLanguage_returnsFailure() {
    var executeResult = Scripts.execute("unknown", "1+1", Collections.emptyMap());
    assertTrue(executeResult.isFailure());
    assertTrue(executeResult.getCause() instanceof IllegalArgumentException);
    assertTrue(executeResult.getCause().getMessage().contains("Unsupported language: unknown"));

    var compiled = Scripts.compile("unknown", "1+1");
    var compileResult = compiled.apply(Collections.emptyMap());
    assertTrue(compileResult.isFailure());
    assertTrue(compileResult.getCause() instanceof IllegalArgumentException);
    assertTrue(compileResult.getCause().getMessage().contains("Unsupported language: unknown"));
  }
}
