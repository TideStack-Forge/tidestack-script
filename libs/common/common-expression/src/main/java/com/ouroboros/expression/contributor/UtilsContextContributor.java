package com.ouroboros.expression.contributor;

import java.util.HashMap;
import java.util.Map;

import com.ouroboros.expression.ExpressionContextContributor;
import com.ouroboros.util.Converter;
import com.ouroboros.util.Dates;

public class UtilsContextContributor implements ExpressionContextContributor {
  @Override
  public Map<String, Object> getBindings() {
    var map = new HashMap<String, Object>();
    map.put("Dates", new Dates());
    map.put("Converter", new Converter());
    return map;
  }
}
