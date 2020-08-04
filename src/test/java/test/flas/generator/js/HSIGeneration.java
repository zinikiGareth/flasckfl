package test.flas.generator.js;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSIfCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.ExtractField;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.mock.IndentWriter;

public class HSIGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	JSStorage jse = context.mock(JSStorage.class);
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	IndentWriter w = new IndentWriter(pw);

	@Test
	public void varsAreNamedAccordingToSlotPosition() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class);
		JSGenerator gen = JSGenerator.forTests(meth, runner, null);
		Slot slot = new ArgSlot(3, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(slot));
		context.checking(new Expectations() {{
			oneOf(meth).bindVar(slot, "_0", "x");
		}});
		gen.bind(slot , "x");
	}
	
	@Test
	public void headProducesAnEvalStatement() {
		JSMethod meth = new JSMethod(jse, null, false, "fred");
		meth.argument("_cxt");
		meth.argument("_0");
		meth.head("_0");
		meth.write(w, (ByteCodeEnvironment)null);
		assertEquals("\nnull.fred = function(_cxt, _0) {\n  _0 = _cxt.head(_0);\n}\n\nnull.fred.nfargs = function() { return 1; }\n", sw.toString());
	}

	@Test
	public void ifCtorProducesAnIfWithTwoBlocks() {
		JSMethod meth = new JSMethod(jse, null, false, "fred");
		meth.argument("_cxt");
		JSIfCreator ifCtor = meth.ifCtor("_0", "Nil");
		ifCtor.trueCase().returnObject(ifCtor.trueCase().string("hello"));
		ifCtor.falseCase().returnObject(ifCtor.falseCase().string("other"));
		ifCtor.write(w, null);
		assertEquals("if (_cxt.isA(_0, 'Nil')) {\n  return 'hello';\n} else \n  return 'other';\n", sw.toString());
	}

	@Test
	public void ifConstProducesAnIfWithTwoBlocks() {
		JSMethod meth = new JSMethod(jse, null, false, "fred");
		meth.argument("_cxt");
		JSIfCreator ifConst = meth.ifConst("_0", "hello");
		ifConst.trueCase().returnObject(ifConst.trueCase().string("hello"));
		ifConst.falseCase().returnObject(ifConst.falseCase().string("other"));
		ifConst.write(w, null);
		assertEquals("if ((_0 == 'hello')) {\n  return 'hello';\n} else \n  return 'other';\n", sw.toString());
	}

	@Test
	public void ifTrueProducesAnIfWithTwoBlocks() {
		JSMethod meth = new JSMethod(jse, null, false, "fred");
		meth.argument("_cxt");
		JSIfCreator ifTrue = meth.ifTrue(new JSLiteral("true"));
		ifTrue.trueCase().returnObject(ifTrue.trueCase().string("hello"));
		ifTrue.falseCase().returnObject(ifTrue.falseCase().string("other"));
		ifTrue.write(w, null);
		assertEquals("if (_cxt.isTruthy(true)) {\n  return 'hello';\n} else \n  return 'other';\n", sw.toString());
	}

	@Test
	public void errorCreatesAnError() {
		JSMethod meth = new JSMethod(jse, null, false, "fred");
		meth.argument("_cxt");
		meth.argument("_0");
		meth.errorNoCase();
		meth.write(w, (ByteCodeEnvironment)null);
		assertEquals("\nnull.fred = function(_cxt, _0) {\n  return FLError.eval(_cxt, 'no matching case');\n}\n\nnull.fred.nfargs = function() { return 1; }\n", sw.toString());
	}
	
	@Test
	public void errorNoDefaultGuardCreatesAnError() {
		JSMethod meth = new JSMethod(jse, null, false, "fred");
		meth.argument("_cxt");
		meth.argument("_0");
		meth.errorNoDefaultGuard();
		meth.write(w, (ByteCodeEnvironment)null);
		assertEquals("\nnull.fred = function(_cxt, _0) {\n  return FLError.eval(_cxt, 'no default guard');\n}\n\nnull.fred.nfargs = function() { return 1; }\n", sw.toString());
	}
	
	@Test
	public void fieldsCanBeExtracted() {
		ExtractField ef = new ExtractField("_1", "_0", "head");
		assertEquals("_1", ef.asVar());
		ef.write(new IndentWriter(new PrintWriter(sw)), null);
		assertEquals("var _1 = _cxt.field(_0, 'head');\n", sw.toString());
	}
}
