package com.ouroboros.expression.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 覆盖“缺失键导致表达式实例被永久污染”的缺陷。
 * <p>{@code SpElImpl} 原先同时注册 Spring 的 {@code MapAccessor} 与恒真的
 * {@code NullReturningPropertyAccessor}。当某次求值中某个 Map 键缺失时，
 * {@code MapAccessor} 抛异常、SpEL 回退到兜底访问器，并把该结论缓存进 AST 节点；
 * 之后即使键存在，同一表达式实例也永远取到 null。
 * <p>真实表现：先请求一次不带 {@code orderBy} 的列表（键缺失），
 * 之后所有带 {@code orderBy} 的排序请求都被忽略并回退到默认排序。
 */
public class SpElImplAccessorFallbackTest {

  /** 排序映射节点的真实表达式（来自 logic-flow/search 模板）。 */
  private static final String ORDER_BODY =
      "($0.orderBy ?: '').isEmpty() ? $default : $0.orderBy + ' ' + (($0.orderDir ?: '').isEmpty() ? 'ASC' : $0.orderDir)";

  private static Map<String, Object> contextWithOrderBy() {
    Map<String, Object> picked = new LinkedHashMap<>();
    picked.put("orderBy", "positionNo");
    picked.put("orderDir", "asc");
    return context(picked);
  }

  private static Map<String, Object> contextWithoutOrderBy() {
    Map<String, Object> picked = new LinkedHashMap<>();
    picked.put("page", "1");
    return context(picked);
  }

  private static Map<String, Object> context(Map<String, Object> picked) {
    Map<String, Object> defaultOrder = new LinkedHashMap<>();
    defaultOrder.put("createdAt", "DESC");

    Map<String, Object> context = new LinkedHashMap<>();
    context.put("$0", picked);
    context.put("$1", defaultOrder);
    context.put("$default", defaultOrder);
    return context;
  }

  @Test
  public void orderByShouldWorkAfterARequestWithoutOrderBy() {
    SpElImpl el = new SpElImpl();
    String expression = "${ " + ORDER_BODY + " }";

    assertEquals("positionNo asc", String.valueOf(el.apply(expression, contextWithOrderBy()).get()));
    assertEquals("{createdAt=DESC}", String.valueOf(el.apply(expression, contextWithoutOrderBy()).get()));
    assertEquals("positionNo asc", String.valueOf(el.apply(expression, contextWithOrderBy()).get()));
    assertEquals("positionNo asc", String.valueOf(el.apply(expression, contextWithOrderBy()).get()));
  }

  @Test
  public void missingKeyShouldNotPoisonLaterReadsOfSameKey() {
    SpElImpl el = new SpElImpl();
    String expression = "${ ($0.orderBy ?: '').isEmpty() ? $default : $0.orderBy }";

    assertEquals("{createdAt=DESC}", String.valueOf(el.apply(expression, contextWithoutOrderBy()).get()));
    assertEquals("positionNo", String.valueOf(el.apply(expression, contextWithOrderBy()).get()));
  }

  @Test
  public void nullValuedKeyIsReadAsNullWithoutThrowing() {
    Map<String, Object> picked = new LinkedHashMap<>();
    picked.put("orderBy", null);
    picked.put("orderDir", "asc");

    var result = new SpElImpl().apply("${ $0.orderBy }", context(picked));

    assertTrue(result.isRight(), String.valueOf(result));
    assertEquals(null, result.get());
  }

  @Test
  public void missingPropertyOnPlainMapIsReadAsNull() {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("existing", "value");

    var result = new SpElImpl().apply("${ missing }", context);

    assertTrue(result.isRight(), String.valueOf(result));
    assertEquals(null, result.get());
  }
}
