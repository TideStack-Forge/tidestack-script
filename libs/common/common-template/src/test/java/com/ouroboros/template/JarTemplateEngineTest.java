package com.ouroboros.template;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JarTemplateEngineTest {

  @Test
  void generate_keepsJarResourceTemplateBehavior() {
    var engine = new JarTemplateEngine("templates/common-template-test");

    var result = engine.generate("greeting", Map.of("name", "Ouroboros"));

    assertEquals("{\"message\":\"Hello Ouroboros\"}", result);
  }
}
