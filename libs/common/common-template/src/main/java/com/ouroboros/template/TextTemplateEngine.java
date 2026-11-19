package com.ouroboros.template;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class TextTemplateEngine {

  private static final String INLINE_TEMPLATE_NAME = "inline-text-template";

  private final Configuration configuration;

  public TextTemplateEngine() {
    this.configuration = createConfiguration();
  }

  /** Parse only: no variable lookup, template processing, or include loading. */
  public void validateSyntax(String templateText) throws IOException {
    parse(templateText);
  }

  private Template parse(String templateText) throws IOException {
    Objects.requireNonNull(templateText, "templateText must not be null");
    return new Template(INLINE_TEMPLATE_NAME, new StringReader(templateText), configuration);
  }

  public String render(String templateText, Map<String, ?> parameters) {
    Objects.requireNonNull(templateText, "templateText must not be null");
    Objects.requireNonNull(parameters, "parameters must not be null");

    try {
      var template = parse(templateText);
      var writer = new StringWriter();
      template.process(parameters, writer);
      return writer.toString();
    } catch (IOException | TemplateException | RuntimeException e) {
      throw new TextTemplateException("Failed to render text template", e);
    }
  }

  private static Configuration createConfiguration() {
    var cfg = new Configuration(Configuration.VERSION_2_3_31);
    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);
    cfg.setWrapUncheckedExceptions(true);
    cfg.setFallbackOnNullLoopVariable(false);
    cfg.setAPIBuiltinEnabled(false);
    cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
    return cfg;
  }
}
