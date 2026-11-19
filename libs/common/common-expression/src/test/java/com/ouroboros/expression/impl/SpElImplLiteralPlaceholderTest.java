package com.ouroboros.expression.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.expression.EL;

/**
 * 覆盖“单占位符快路径把字面量当变量名求值成 null”的缺陷。
 * <p>{@code SpElImpl} 原先对 {@code ${xxx}} 这类单占位符直接按变量名去上下文取值，取不到就返回 null。
 * 但业务模型字段的默认值会被 {@code DefaultDataModelField#getDefaultValue} 包装成 {@code ${ 0 }} 这种形式，
 * {@code 0} 也满足单占位符的正则，于是被当作“变量 0”求值成 null，而不是数字 0。
 * <p>真实表现：默认值为 {@code "0"} 且 {@code isRequired=true} 的字段（例如 {@code device.info} 的
 * {@code enabled}）在未提交该字段时，必填规则拿到 null，新增接口返回
 * {@code {"code":422,"msg":"校验失败","errors":{"enabled":"是否启用不能为空"}}}。
 */
public class SpElImplLiteralPlaceholderTest {

  @Test
  public void numericLiteralShouldEvaluateToNumber() {
    SpElImpl el = new SpElImpl();

    assertEquals(Integer.valueOf(0), el.apply(EL.wrapExpression("0"), Collections.emptyMap()).get());
    assertEquals(Integer.valueOf(1), el.apply(EL.wrapExpression("1"), Collections.emptyMap()).get());
    assertEquals(Double.valueOf(1.5), el.apply("${ 1.5 }", Collections.emptyMap()).get());
  }

  @Test
  public void booleanAndNullLiteralsShouldEvaluateToTheirLiteralValue() {
    SpElImpl el = new SpElImpl();

    assertEquals(Boolean.TRUE, el.apply("${ true }", Collections.emptyMap()).get());
    assertEquals(null, el.apply("${ null }", Collections.emptyMap()).get());
  }

  @Test
  public void contextVariableShouldStillBeReadFromContext() {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("sn", "123");

    assertEquals("123", new SpElImpl().apply(EL.wrapExpression("sn"), context).get());
  }

  @Test
  public void missingSimpleKeyShouldStillEvaluateToNull() {
    var result = new SpElImpl().apply("${ missingKey }", Collections.emptyMap());

    assertTrue(result.isRight(), String.valueOf(result));
    assertEquals(null, result.get());
  }

  @Test
  public void bareWordPlaceholderKeepsPropertySemantics() {
    // 裸词在 SpEL 里就是属性引用，仍然取不到值：
    // 非数字的默认值必须在业务模型里写成 'EFFECTIVE' 这种带引号的字面量
    var result = new SpElImpl().apply("${ EFFECTIVE }", Collections.emptyMap());

    assertTrue(result.isRight(), String.valueOf(result));
    assertEquals(null, result.get());
  }
}
