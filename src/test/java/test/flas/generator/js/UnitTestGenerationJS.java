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
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class UnitTestGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.something");

	@Test
	public void weDoActuallyCreateATestCaseFunction() {
		JSStorage jse = context.mock(JSStorage.class);
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(jse).newFunction("test.something._ut_package", false, "_ut4"); will(returnValue(meth));
			oneOf(meth).argument("_cxt");
			oneOf(meth).argument("runner");
		}});
		JSGenerator gen = new JSGenerator(jse, null);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		gen.visitUnitTest(utc);
		context.checking(new Expectations() {{
			// I don't currently think this requires any work ...
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
	public void weCanCreateLocalUDDMockContracts() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		NestedVisitor nv = context.mock(NestedVisitor.class);
		JSFunctionState state = context.mock(JSFunctionState.class);
		JSExpr mc = context.mock(JSExpr.class, "mockContract");
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Ctr"));
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ctr.bind(cd);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, utn, "data"), null);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(JSGenerator.class)));
			oneOf(meth).mockContract(cd.name()); will(returnValue(mc));
			oneOf(state).addMock(udd, mc);
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, runner, nv, state));
		gen.visitUnitDataDeclaration(udd);
	}
	
	@Test
	public void weCanCreateLocalObjectsInUDDs() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		NestedVisitor nv = context.mock(NestedVisitor.class);
		JSFunctionState state = context.mock(JSFunctionState.class);
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), false, new ArrayList<>());
		TypeReference tr = new TypeReference(pos, "Obj");
		tr.bind(od);
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, utn, "data"), null);
		JSExpr mo = context.mock(JSExpr.class, "mockObject");
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(JSGenerator.class)));
			oneOf(meth).createObject(od.name()); will(returnValue(mo));
			oneOf(state).addMock(udd, mo);
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, runner, nv, state));
		gen.visitUnitDataDeclaration(udd);
	}
	
	@Test
	public void weVisitAnAssertStep() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr runner = context.mock(JSExpr.class, "runner");
		NumericLiteral lhs = new NumericLiteral(pos, "42", 2);
		StringLiteral rhs = new StringLiteral(pos, "hello");
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(JSGenerator.class)));
			oneOf(nv).push(with(any(CaptureAssertionClauseVisitorJS.class)));
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, runner, nv));
		UnitTestAssert a = new UnitTestAssert(lhs, rhs);
		gen.visitUnitTestAssert(a);
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
	}
}
