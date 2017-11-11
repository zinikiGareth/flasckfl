package org.flasck.flas.jsgen;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestGeneratingTake {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();

	/* Input:
"take" 2/5 [-, Cons, Nil]
HEAD v1
SWITCH v1 Cons
  BIND v2 v1.head
  BIND v3 v1.tail
  HEAD v0
  SWITCH v0 Number
    IF v0 0
      RETURN Nil
  RETURN v6 [v4, v5]
SWITCH v1 Nil
  RETURN Nil
ERROR
CLOSURE v4
  PUSH -
  PUSH v0
  PUSH 1
CLOSURE v5
  PUSH take
  PUSH v4
  PUSH v3
CLOSURE v6
  PUSH Cons
  PUSH v2
  PUSH v5

	 * Output:

StdLib.take = function(v0, v1) {
	v1 = FLEval.head(v1);
	if (v1 instanceof FLError)
		return v1;
	n = FLEval.head(n);
	if (n instanceof FLError)
		return n;
	if (FLEval.isInteger(n) && l instanceof List) {
		if (n === 0)
			return List.nil;
		if (l._ctor === 'nil')
			return List.nil;
		if (l._ctor === 'cons')
			return FLEval.closure(
				List.cons,
				l.head,
				FLEval.closure(
					StdLib.take,
					FLEval.closure(
						FLEval.minus,
						n,
						1
					),
					l.tail
				)
			);
	}
	return FLEval.error("take: case not handled");
}
	 */
	@Test
	public void test() {
		HSIEForm input = HSIETestData.take(context);
		new Generator(new JSTarget("ME")).generate(input);
	}
}
