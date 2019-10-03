package test.flas.generator.jvm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class UnitTestGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final MethodDefiner meth = context.mock(MethodDefiner.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(meth).lenientMode(with(any(Boolean.class)));
		}});
	}
	
	@Test
	public void weDoActuallyCreateATestCaseClass() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		context.checking(new Expectations() {{
			allowing(bcc).generateAssociatedSourceFile();
			oneOf(meth).nextLocal(); will(returnValue(6));
		}});
		Var arg = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.something._ut_package._ut4"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "void", "dotest"); will(returnValue(meth));
			oneOf(meth).argument("org.flasck.flas.testrunner.JVMRunner", "runner"); will(returnValue(arg));
			oneOf(meth).getField(arg, "cxt");
		}});
		StackVisitor sv = new StackVisitor();
		JVMGenerator gen = new JVMGenerator(bce, sv);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.something"), "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		gen.visitUnitTest(utc);
		context.checking(new Expectations() {{
			oneOf(meth).returnVoid();
			oneOf(bcc).generate();
		}});
		gen.leaveUnitTest(utc);
	}
	
	@Test
	public void weVisitAnAssertStep() {
		NumericLiteral lhs = new NumericLiteral(pos, 42);
		StringLiteral rhs = new StringLiteral(pos, "hello");
		IExpr runner = context.mock(IExpr.class, "runner");
		IExpr iv = context.mock(IExpr.class, "iv");
		IExpr dv = context.mock(IExpr.class, "dv");
		IExpr l1 = context.mock(IExpr.class, "lhs");
		IExpr la = context.mock(IExpr.class, "la");
		IExpr r1 = context.mock(IExpr.class, "rhs");
		IExpr ra = context.mock(IExpr.class, "ra");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		IExpr asv = context.mock(IExpr.class, "asv");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(dv));
			oneOf(meth).intConst(42); will(returnValue(iv));
			oneOf(meth).box(iv); will(returnValue(biv));
			oneOf(meth).castTo(dv, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(l1));
			oneOf(meth).stringConst("hello"); will(returnValue(r1));
			oneOf(meth).as(l1, "java.lang.Object"); will(returnValue(la));
			oneOf(meth).as(r1, "java.lang.Object"); will(returnValue(ra));
			oneOf(meth).callVirtual("void", runner, "assertSameValue", ra, la); will(returnValue(asv));
			oneOf(asv).flush();
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, runner, null).stackVisitor());
		UnitTestAssert a = new UnitTestAssert(lhs, rhs);
		gen.visitUnitTestAssert(a);
	}
}
