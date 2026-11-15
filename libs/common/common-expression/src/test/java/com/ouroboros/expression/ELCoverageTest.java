package com.ouroboros.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ELCoverageTest {

  @Test
  public void evalReturnsTypedRightValueWhenCastMatches() {
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("number", 6);

    assertEquals(Integer.valueOf(6), EL.eval(Integer.class, "${ number }", context).get());
  }

  @Test
  public void evalReturnsLeftWhenCastDoesNotMatch() {
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("number", 6);

    assertTrue(EL.eval(String.class, "${ number }", context).isLeft());
  }

  @Test
  public void wrapExpressionLeavesWrappedAndNullInputsUntouched() {
    assertEquals(null, EL.wrapExpression(null));
    assertEquals("${ number }", EL.wrapExpression("${ number }"));
    assertEquals("${ number }", EL.wrapExpression("number"));
  }

  @Test
  public void evalDelegatesToDefaultImplementation() {
    assertTrue(EL.eval("${ 1 + 1 }", Collections.<String, Object>emptyMap()).isRight());
    assertEquals(2, EL.eval("${ 1 + 1 }", Collections.<String, Object>emptyMap()).get());
  }
}
