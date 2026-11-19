package com.ouroboros.template;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextTemplateEngineTest {

  private final TextTemplateEngine engine = new TextTemplateEngine();

  @Test
  void syntaxValidationDoesNotRenderUnknownValuesOrLoadIncludes() {
    assertDoesNotThrow(() -> engine.validateSyntax("${unknown.call()}<#include 'missing-template.ftl'>"));
    assertThrows(freemarker.core.ParseException.class, () -> engine.validateSyntax("<#if active>${name}"));
    assertDoesNotThrow(() -> engine.validateSyntax("<#noparse>${literal</#noparse>"));
  }

  @Test
  void render_supportsPlainTextVariablesConditionsAndLoops() {
    var result = engine.render("""
        Hello ${name}!
        <#if enabled>
        Items:<#list items as item> ${item}</#list>
        </#if>
        """, Map.of(
        "name", "Ouroboros",
        "enabled", true,
        "items", List.of("react", "vue")));

    assertEquals("""
        Hello Ouroboros!
        Items: react vue
        """, result);
  }

  @Test
  void render_preservesLiteralTemplateSyntaxWithFreemarkerNoparse() {
    var result = engine.render("<#noparse>${project.version}</#noparse>", Map.of());

    assertEquals("${project.version}", result);
  }
}
