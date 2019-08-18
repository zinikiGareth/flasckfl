package test.flas.generator.js;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.JSClassCreator;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class UnitTestGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);

	@Test
	public void weDoActuallyCreateATestCaseFunction() {
		JSStorage jse = context.mock(JSStorage.class);
		JSClassCreator jsc = context.mock(JSClassCreator.class);
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(jse).newFunction("test.something._ut_package", "_ut4"); will(returnValue(meth));
			oneOf(meth).argument("runner");
		}});
		JSGenerator gen = new JSGenerator(jse);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.something"), "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		gen.visitUnitTest(utc);
		context.checking(new Expectations() {{
			// I don't currently think this requires any work ...
		}});
		gen.leaveUnitTest(utc);
	}
	
	@Test
	public void weVisitAnAssertStep() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		NumericLiteral lhs = new NumericLiteral(pos, "42", 2);
		StringLiteral rhs = new StringLiteral(pos, "hello");
		JSExpr la = context.mock(JSExpr.class, "la");
		JSExpr ra = context.mock(JSExpr.class, "ra");
		context.checking(new Expectations() {{
			oneOf(meth).literal("42"); will(returnValue(la));
			oneOf(meth).string("hello"); will(returnValue(ra));
			oneOf(meth).assertable(runner, "assertSameValue", ra, la);
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, runner));
		UnitTestAssert a = new UnitTestAssert(lhs, rhs);
		gen.visitUnitTestAssert(a);
	}
}
