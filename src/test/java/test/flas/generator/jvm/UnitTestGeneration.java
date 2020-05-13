package test.flas.generator.jvm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.jvm.J;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;

public class UnitTestGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final MethodDefiner meth = context.mock(MethodDefiner.class);
	private final PackageName pkg = new PackageName("test.something");
	
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
			oneOf(meth).argument(J.TESTHELPER, "runner"); will(returnValue(arg));
			oneOf(meth).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(arg));
			oneOf(meth).callInterface("void", arg, "clearBody", arg);
		}});
		StackVisitor sv = new StackVisitor();
		JVMGenerator gen = new JVMGenerator(bce, sv, null);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		gen.visitUnitTest(utc);
		context.checking(new Expectations() {{
			oneOf(meth).callInterface("void", arg, "testComplete");
			oneOf(meth).returnVoid();
			oneOf(bcc).generate();
		}});
		gen.leaveUnitTest(utc);
	}
	
	@Test
	@Ignore
	public void weCanCreateLocalUDStringValues() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		IExpr runner = context.mock(IExpr.class, "runner");
		IExpr cls = context.mock(IExpr.class, "cls");
		IExpr call = context.mock(IExpr.class, "call");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(17));
		}});
		AVar v1 = new AVar(meth, J.OBJECT, "v1");
		context.checking(new Expectations() {{
//			oneOf(meth).classConst("test.something.Ctr"); will(returnValue(cls));
//			oneOf(meth).callVirtual(J.OBJECT, runner, "mockContract", cls); will(returnValue(call));
//			oneOf(meth).avar(J.OBJECT, "v1"); will(returnValue(v1));
//			oneOf(meth).assign(v1, call);
		}});
		JVMGenerator jvm = JVMGenerator.forTests(meth, runner, null);
		Traverser gen = new Traverser(jvm.stackVisitor());
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ctr.bind(cd);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, utn, "data"), new StringLiteral(pos, "hello"));
		gen.visitUnitDataDeclaration(udd);
		assertEquals(v1, jvm.state().resolveMock(udd));
	}

	@Test
	public void weCanCreateLocalUDDMockContracts() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		IExpr runner = context.mock(IExpr.class, "runner");
		IExpr cls = context.mock(IExpr.class, "cls");
		IExpr call = context.mock(IExpr.class, "call");
		IExpr fcx = context.mock(IExpr.class, "fcx");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(17));
		}});
		AVar v1 = new AVar(meth, J.OBJECT, "v1");
		context.checking(new Expectations() {{
			oneOf(meth).castTo(fcx, J.ERRORCOLLECTOR); will(returnValue(runner));
			oneOf(meth).classConst("test.something.Ctr"); will(returnValue(cls));
			oneOf(meth).callInterface(J.OBJECT, fcx, "mockContract", runner, cls); will(returnValue(call));
			oneOf(meth).avar(J.OBJECT, "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, call);
		}});
		JVMGenerator jvm = JVMGenerator.forTests(meth, fcx, null);
		Traverser gen = new Traverser(jvm.stackVisitor());
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ctr.bind(cd);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, utn, "data"), null);
		gen.visitUnitDataDeclaration(udd);
		assertEquals(v1, jvm.state().resolveMock(udd));
	}

	@Test
	public void weCanCreateLocalObjectsInUDDs() {
		IExpr runner = context.mock(IExpr.class, "runner");
		IExpr call = context.mock(IExpr.class, "call");
		IExpr stored = context.mock(IExpr.class, "stored");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(17));
		}});
		AVar v1 = new AVar(meth, J.OBJECT, "v1");
		context.checking(new Expectations() {{
			oneOf(meth).callStatic("test.something.Obj", J.OBJECT, "eval", runner); will(returnValue(call));
			oneOf(meth).as(call, J.OBJECT); will(returnValue(call));
			oneOf(meth).callInterface(J.OBJECT, runner, "storeMock", call); will(returnValue(stored));
			oneOf(meth).avar(J.OBJECT, "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, stored);
		}});
		JVMGenerator jvm = JVMGenerator.forTests(meth, runner, null);
		Traverser gen = new Traverser(jvm.stackVisitor());
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), false, new ArrayList<>());
		TypeReference tr = new TypeReference(pos, "Obj");
		tr.bind(od);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, utn, "data"), null);
		gen.visitUnitDataDeclaration(udd);
		assertEquals(v1, jvm.state().resolveMock(udd));
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
			oneOf(meth).callInterface("void", runner, "assertSameValue", runner, ra, la); will(returnValue(asv));
			oneOf(asv).flush();
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, runner, null).stackVisitor());
		UnitTestAssert a = new UnitTestAssert(lhs, rhs);
		gen.visitUnitTestAssert(a);
	}
}
