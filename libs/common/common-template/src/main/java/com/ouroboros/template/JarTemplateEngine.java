package com.ouroboros.template;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import io.vavr.control.Try;

import com.ouroboros.expression.ContextWrappingHelper;
import com.ouroboros.util.Json;
import com.ouroboros.util.ResourceUtil;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class JarTemplateEngine {
  final Map<String, Object> wrapper = new HashMap<>();
  private Configuration cfg;

  public JarTemplateEngine(String templateResourcePath) {
    this(templateResourcePath, ".json.ftl");
  }

  public JarTemplateEngine(String templateResourcePath, String suffix) {
    var finalPath = templateResourcePath.startsWith("/") ? templateResourcePath.substring(1) : templateResourcePath;
    cfg = new Configuration(Configuration.VERSION_2_3_31);
    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateLoader(new JarTemplateLoader(finalPath, suffix));
    var jsonUtils = new Json();
    wrapper.put("Jsons", jsonUtils);
    wrapper.put("Json", jsonUtils);
    wrapper.put("JsonUtils", jsonUtils);
  }

  public String generate(String templateName, Map<String, ?> config) {
    var wrappedData = wrapData(config);
    return Try
        .of(() -> {
          Template tpl = cfg.getTemplate(templateName);
          var stringWriter = new StringWriter();
          tpl.process(wrappedData, stringWriter);
          return stringWriter.toString();
        }).toEither()
        .peekLeft(e -> e.printStackTrace())
        .getOrElse((String) null);
  }

  public <T> T generate(String templateName, Map<String, ?> config, Class<T> clazz) {
    var resultStr = generate(templateName, config);
    return Optional.ofNullable(resultStr)
        .filter(StringUtils::isNotBlank)
        .map(str -> Json.tryToBean(clazz, str).toEither().peekLeft(e -> e.printStackTrace()).getOrNull())
        .orElse(null);
  }

  private Map<String, Object> wrapData(Map<String, ?> data) {
    return new ContextWrappingHelper((Map<String, Object>) data, wrapper);
  }

  static class JarTemplateLoader implements TemplateLoader {
    final String TEMPLATE_PATH;
    final String SUFFIX;

    public JarTemplateLoader(String templatePath, String suffix) {
      this.TEMPLATE_PATH = templatePath;
      this.SUFFIX = suffix;
    }

    public JarTemplateLoader(String templatePath) {
      this(templatePath, ".json.ftl");
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
      var path = Paths.get(TEMPLATE_PATH, name + SUFFIX);
      return ResourceUtil.getResourceContentStream(path.toString())
          .findFirst()
          .orElse(null);
    }

    @Override
    public long getLastModified(Object templateSource) {
      return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
      return new StringReader(templateSource instanceof String str ? str : "");
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {

    }
  }
}
