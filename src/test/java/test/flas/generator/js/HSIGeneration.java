package test.flas.generator.js;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.flasck.flas.compiler.jsgen.JSIfExpr;
import org.flasck.flas.compiler.jsgen.JSMethod;
import org.junit.Test;
import org.zinutils.bytecode.mock.IndentWriter;

public class HSIGeneration {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	IndentWriter w = new IndentWriter(pw);

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
	
}
