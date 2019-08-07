package test.flas.generator.jvm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class ExpressionGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aSimpleInteger() {
		MethodDefiner meth = context.mock(MethodDefiner.class);
		NumericLiteral expr = new NumericLiteral(pos, 42);
		IExpr dv = context.mock(IExpr.class, "dv");
		IExpr iv = context.mock(IExpr.class, "iv");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(dv));
			oneOf(meth).intConst(42); will(returnValue(iv));
			oneOf(meth).box(iv); will(returnValue(biv));
			oneOf(meth).castTo(dv, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv);
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr);
	}

	@Test
	public void aSimpleString() {
		MethodDefiner meth = context.mock(MethodDefiner.class);
		StringLiteral expr = new StringLiteral(pos, "hello");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello");
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr);
	}

	@Test
	public void aVar() {
		MethodDefiner meth = context.mock(MethodDefiner.class);
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 0));
		context.checking(new Expectations() {{
			oneOf(meth).callStatic("test.repo.PACKAGEFUNCTIONS$x", "java.lang.Object", "eval", new IExpr[0]);
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr);
	}
}
