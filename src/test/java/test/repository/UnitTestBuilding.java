package test.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.junit.Test;

import flas.matchers.ExprMatcher;
import flas.matchers.TypeReferenceMatcher;

public class UnitTestBuilding {
	private InputPosition pos = new InputPosition("fred", 10, 0, "hello");
	private PackageName pkg = new PackageName("test.pkg");
	private UnitTestFileName utfn = new UnitTestFileName(pkg, "unit");
	private UnitTestName name = new UnitTestName(utfn, 4);

	@Test
	public void addingAFieldToADataDecl() {
		UnitDataDeclaration decl = new UnitDataDeclaration(pos, false, null, null, null);
		decl.field(new UnresolvedVar(pos, "x"), new StringLiteral(pos, "hello"));
		assertEquals(1, decl.fields.size());
		Assignment f = decl.fields.get(0);
		assertEquals("x", f.field.var);
		assertThat(f.value, ExprMatcher.string("hello"));
	}
	
	@Test
	public void constructingATestCase() {
		UnitTestCase utc = new UnitTestCase(name, "this is a test");
		assertEquals("test.pkg.unit._ut4", utc.name.uniqueName());
		assertEquals("this is a test", utc.description);
	}
	
	@Test
	public void addingAnAssertStep() {
		UnitTestCase utc = new UnitTestCase(name, "this is a test");
		utc.assertion(new StringLiteral(pos, "hello"), new StringLiteral(pos, "goodbye"));
		assertEquals(1, utc.steps.size());
		assertTrue(utc.steps.get(0) instanceof UnitTestAssert);
		UnitTestAssert a = (UnitTestAssert) utc.steps.get(0);
		assertThat(a.expr, ExprMatcher.string("hello"));
		assertThat(a.value, ExprMatcher.string("goodbye"));
	}
	
	@Test
	public void addingADataStep() {
		UnitTestCase utc = new UnitTestCase(name, "this is a test");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, new TypeReference(pos, "Unit"), FunctionName.function(pos, utc.name, "x"), new StringLiteral(pos, "expr"));
		utc.data(udd);
		assertEquals(1, utc.steps.size());
		assertEquals(udd, utc.steps.get(0));
	}
	
	@Test
	public void canAddMultipleSteps() {
		UnitTestCase utc = new UnitTestCase(name, "this is a test");
		utc.assertion(new StringLiteral(pos, "hello"), new StringLiteral(pos, "goodbye"));
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, new TypeReference(pos, "Unit"), FunctionName.function(pos, utc.name, "x"), new StringLiteral(pos, "expr"));
		utc.data(udd);
		assertEquals(2, utc.steps.size());
		assertTrue(utc.steps.get(0) instanceof UnitTestAssert);
		UnitTestAssert a = (UnitTestAssert) utc.steps.get(0);
		assertThat(a.expr, ExprMatcher.string("hello"));
		assertThat(a.value, ExprMatcher.string("goodbye"));
		assertEquals(udd, utc.steps.get(1));
	}

	@Test
	public void addingAnEventStep() {
		UnitTestCase utc = new UnitTestCase(name, "this is a test");
		utc.event(new UnresolvedVar(pos, "x"), new TargetZone(pos, "zone", Arrays.asList("zone")), new StringLiteral(pos, "goodbye"));
		assertEquals(1, utc.steps.size());
		assertTrue(utc.steps.get(0) instanceof UnitTestEvent);
		UnitTestEvent ev = (UnitTestEvent) utc.steps.get(0);
		assertThat(ev.card, ExprMatcher.unresolved("x"));
		assertThat(ev.expr, ExprMatcher.string("goodbye"));
	}

	@Test
	public void addingAnInvokeStep() {
		UnitTestCase utc = new UnitTestCase(name, "this is a test");
		ApplyExpr expr = new ApplyExpr(pos, new MemberExpr(pos, new UnresolvedVar(pos, "obj"), new UnresolvedVar(pos, "meth")), new StringLiteral(pos, "hello"));
		utc.invokeObjectMethod(expr);
		assertEquals(1, utc.steps.size());
		assertTrue(utc.steps.get(0) instanceof UnitTestInvoke);
		UnitTestInvoke invoke = (UnitTestInvoke) utc.steps.get(0);
		assertEquals(expr, invoke.expr);
	}

	@Test
	public void addingAContractStep() {
		UnitTestCase utc = new UnitTestCase(name, "this is a test");
		utc.sendOnContract(new UnresolvedVar(pos, "x"), new TypeReference(pos, "ContractName"), new UnresolvedVar(pos, "method"));
		assertEquals(1, utc.steps.size());
		assertTrue(utc.steps.get(0) instanceof UnitTestSend);
		UnitTestSend ev = (UnitTestSend) utc.steps.get(0);
		assertThat(ev.card, ExprMatcher.unresolved("x"));
		assertThat(ev.contract, TypeReferenceMatcher.type("ContractName"));
		assertThat(ev.expr, ExprMatcher.unresolved("method"));
	}
	
}
