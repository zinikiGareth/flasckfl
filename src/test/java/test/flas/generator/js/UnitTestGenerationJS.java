package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.CaptureAssertionClauseVisitorJS;
import org.flasck.flas.compiler.jsgen.JSFunctionState;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class UnitTestGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.something");

	@Test
	@Ignore
	public void weDoActuallyCreateATestCaseFunction() {
		JSStorage jse = context.mock(JSStorage.class);
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSGenerator gen = new JSGenerator(null, jse, null, null);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		context.checking(new Expectations() {{
			oneOf(jse).newFunction(utn, "test.something._ut_package", utfn, false, "_ut4"); will(returnValue(meth));
			oneOf(meth).clear();
			oneOf(meth).argument("runner");
			oneOf(meth).initContext(false);
		}});
		gen.visitUnitTest(utc);
		context.checking(new Expectations() {{
			oneOf(meth).testComplete();
		}});
		gen.leaveUnitTest(utc);
	}
	
	@Test
	@Ignore // this test is not one I need right now and seems like it needs a lot of thinking
	public void weCanCreateLocalUDDStringValues() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		NestedVisitor nv = context.mock(NestedVisitor.class);
		JSFunctionState state = context.mock(JSFunctionState.class);
		JSExpr sl = context.mock(JSExpr.class, "literal");
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, LoadBuiltins.stringTR, FunctionName.function(pos, utn, "data"), new StringLiteral(pos, "hello"));
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(JSGenerator.class)));
			oneOf(meth).literal("hello"); will(returnValue(sl));
			oneOf(state).addMock(udd, sl);
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, runner, nv, state));
		gen.visitUnitDataDeclaration(udd);
	}
	
	@Test
	@Ignore
	public void weCanCreateLocalUDDMockContracts() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		NestedVisitor nv = new StackVisitor();
		JSFunctionState state = context.mock(JSFunctionState.class);
		JSExpr mc = context.mock(JSExpr.class, "mockContract");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		TypeReference ctr = new TypeReference(pos, "Ctr.Up");
		ctr.bind(cd);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, utn, "data"), null);
		context.checking(new Expectations() {{
//			oneOf(nv).push(with(any(JSGenerator.class)));
			oneOf(meth).mockContract(cd.name()); will(returnValue(mc));
			oneOf(state).addMock(udd, mc);
		}});
		JSGenerator.forTests(meth, runner, nv, state);
		Traverser gen = new Traverser(nv);
		gen.visitUnitTestStep(udd);
	}
	
	@Test
	@Ignore
	public void weCanCreateLocalObjectsInUDDs() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		StackVisitor nv = new StackVisitor();
		JSFunctionState state = context.mock(JSFunctionState.class);
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), false, new ArrayList<>());
		TypeReference tr = new TypeReference(pos, "Obj");
		tr.bind(od);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, utn, "data"), null);
		JSExpr mo = context.mock(JSExpr.class, "mockObject");
		JSExpr smo = context.mock(JSExpr.class, "storedObject");
		context.checking(new Expectations() {{
			oneOf(meth).createObject(od.name()); will(returnValue(mo));
			oneOf(meth).storeMockObject(udd, mo); will(returnValue(smo));
			oneOf(state).addMock(udd, smo);
		}});
		JSGenerator.forTests(meth, runner, nv, state);
		Traverser gen = new Traverser(nv);
		gen.visitUnitTestStep(udd);
	}
	
	@Test
	@Ignore // this is just too hard ...
	public void weVisitAnAssertStep() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		NumericLiteral lhs = new NumericLiteral(pos, "42", 2);
		StringLiteral rhs = new StringLiteral(pos, "hello");
		context.checking(new Expectations() {{
			oneOf(meth).string("hello");
			oneOf(meth).literal("42");
		}});
		StackVisitor nv = new StackVisitor();
		JSGenerator.forTests(meth, runner, nv);
		Traverser gen = new Traverser(nv);
		UnitTestAssert a = new UnitTestAssert(lhs, rhs);
		gen.visitUnitTestStep(a);
	}
	
	@Test
	public void theCollectorAssemblesAnAssertStep() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		JSExpr la = context.mock(JSExpr.class, "la");
		JSExpr ra = context.mock(JSExpr.class, "ra");
		JSFunctionState state = context.mock(JSFunctionState.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(CaptureAssertionClauseVisitorJS.class)));
			oneOf(meth).assertable(runner, "assertSameValue", la, ra);
			oneOf(nv).result(null);
		}});
		CaptureAssertionClauseVisitorJS capture = new CaptureAssertionClauseVisitorJS(state, nv, meth, runner);
		capture.result(la);
		capture.result(ra);
		capture.postUnitTestAssert(null);
	}
}
