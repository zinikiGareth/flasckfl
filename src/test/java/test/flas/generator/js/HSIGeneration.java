package test.flas.generator.js;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.flasck.flas.compiler.jsgen.ExtractField;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSIfExpr;
import org.flasck.flas.compiler.jsgen.JSMethod;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.mock.IndentWriter;

public class HSIGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	IndentWriter w = new IndentWriter(pw);

	@Test
	public void varsAreNamedAfterSlotID() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class);
		JSGenerator gen = JSGenerator.forTests(meth, runner, null);
		Slot slot = new ArgSlot(3, null);
		context.checking(new Expectations() {{
			oneOf(meth).bindVar("_3", "x");
		}});
		gen.bind(slot , "x");
	}
	
	@Test
	public void headProducesAnEvalStatement() {
		JSMethod meth = new JSMethod(null, "fred");
		meth.argument("_cxt");
		meth.argument("_0");
		meth.head("_0");
		meth.write(w);
		assertEquals("\nnull.fred = function(_cxt, _0) {\n  _0 = _cxt.head(_0);\n}\n", sw.toString());
	}

	@Test
	public void ifCtorProducesAnIfWithTwoBlocks() {
		JSMethod meth = new JSMethod(null, "fred");
		meth.argument("_cxt");
		JSIfExpr ifCtor = meth.ifCtor("_0", "Nil");
		ifCtor.trueCase().returnObject(ifCtor.trueCase().string("hello"));
		ifCtor.falseCase().returnObject(ifCtor.falseCase().string("other"));
		ifCtor.write(w);
		assertEquals("if (_cxt.isA(_0, 'Nil')) {\n  return 'hello';\n} else {\n  return 'other';\n}\n", sw.toString());
	}

	@Test
	public void errorCreatesAnError() {
		JSMethod meth = new JSMethod(null, "fred");
		meth.argument("_cxt");
		meth.argument("_0");
		meth.errorNoCase();
		meth.write(w);
		assertEquals("\nnull.fred = function(_cxt, _0) {\n  return FLError(_cxt, 'no matching case');\n}\n", sw.toString());
	}
	
	@Test
	public void fieldsCanBeExtracted() {
		ExtractField ef = new ExtractField("_1", "_0", "head");
		assertEquals("_1", ef.asVar());
		ef.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("_1 = _cxt.field(_0, 'head');\n", sw.toString());
	}
}
