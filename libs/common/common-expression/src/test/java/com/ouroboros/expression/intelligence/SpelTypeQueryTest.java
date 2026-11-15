package com.ouroboros.expression.intelligence;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ouroboros.codeintelligence.type.TypeShape;
class SpelTypeQueryTest {
  @Test
  void infersPropertiesIndexesAndLiteralsWithoutEvaluation() {
    var item = TypeShape.object(Map.of("name", TypeShape.scalar("string")), "map");
    var bindings = Map.of("$0", TypeShape.array(item), "customer", item);
    var query = new SpelTypeQuery();
    assertEquals("string", query.infer("$0[0].name", bindings).type());
    assertEquals("string", query.infer("${customer['name']}", bindings).type());
    assertEquals("boolean", query.infer("customer.name != null", bindings).type());
    assertEquals("array", query.infer("{1, 2, 3}", bindings).type());
    assertTrue(query.infer("new java.io.File('/tmp/unwanted').delete()", bindings).isUnknown());
    assertTrue(query.infer("$0.", bindings).isUnknown());
    assertTrue(query.infer("BeanFactory.getBean('something')", bindings).isUnknown());
  }
}
