package test.flas.generator.js.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.JSClass;
import org.flasck.flas.compiler.jsgen.JSClassCreator;
import org.flasck.flas.compiler.jsgen.JSEnvironment;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSLiteral;
import org.flasck.flas.compiler.jsgen.JSMethod;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSVar;
import org.junit.Test;
import org.zinutils.bytecode.mock.IndentWriter;

public class ClassGeneration {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	IndentWriter w = new IndentWriter(pw);
	
	@Test
	public void creatingAClassEnsuresThereIsOnePackageFile() {
		JSEnvironment jse = new JSEnvironment(); // TODO: surely root dir?
		jse.newClass("test.repo", "Fred");
		List<File> acc = new ArrayList<>();
		jse.files().forEach(x -> acc.add(x));
		assertEquals(1, acc.size());
		assertEquals("test.repo", acc.get(0).getPath());
	}

	@Test
	public void creatingAClassReturnsAClassObject() {
		JSEnvironment jse = new JSEnvironment();
		JSClassCreator jcc = jse.newClass("test.repo", "Fred");
		assertNotNull(jcc);
	}

	@Test
	public void aClassCanCreateNewMethods() {
		JSClass jsc = new JSClass(null);
		JSMethodCreator meth = jsc.createMethod("test");
		assertNotNull(meth);
	}

	@Test
	public void methodsCanCreateLiterals() {
		JSMethod jsc = new JSMethod();
		JSExpr expr = jsc.literal("name");
		assertNotNull(expr);
	}

	@Test
	public void methodsCanCreateArguments() {
		JSMethod jsc = new JSMethod();
		JSExpr expr = jsc.argument("v");
		assertNotNull(expr);
		expr.write(w);
		assertEquals("v", sw.toString());
		assertEquals("v", expr.asVar());
	}

	@Test
	public void methodsCanCreateApplyExprs() {
		w = w.indent();
		JSMethod jsc = new JSMethod();
		JSExpr expr = jsc.callMethod(new JSVar("v"), "called", new JSLiteral("true"));
		assertNotNull(expr);
		expr.write(w);
		assertEquals("  const v1 = v.called(true);\n", sw.toString());
	}
}
