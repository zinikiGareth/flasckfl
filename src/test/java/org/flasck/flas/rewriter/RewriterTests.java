package org.flasck.flas.rewriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWEventHandlerDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.junit.Before;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class RewriterTests {
	private final InputPosition posn = new InputPosition("test", 1, 1, null);
	private final ErrorResult errors = new ErrorResult();
	private Rewriter rw;
	private final Scope scope = new Scope(null);
	
	@Before
	public void setup() {
		ImportPackage builtins = Builtin.builtins();
		builtins.define("Timer", Type.builtin(posn, "Timer"));
		rw = new Rewriter(errors, null, builtins);
	}

	@Test
	public void testRewritingSomethingGloballyDefined() {
		scope.define("f", "ME.f", new FunctionCaseDefn(null, CodeType.FUNCTION, "ME.f", new ArrayList<Object>(), new UnresolvedVar(null, "Nil")));
		rw.rewritePackageScope("ME", scope);
		RWFunctionDefinition rfn = rw.functions.get("ME.f");
		assertEquals("ME.f", rfn.name());
		assertTrue(rfn.cases.get(0).expr instanceof PackageVar);
		assertEquals("Nil", ((PackageVar)rfn.cases.get(0).expr).id);
	}

	@Test
	public void testRewritingAParameter() {
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(new VarPattern(null, "x"));
		scope.define("f", "ME.f", new FunctionCaseDefn(null, CodeType.FUNCTION, "ME.f", args, new UnresolvedVar(null, "x")));
		rw.rewritePackageScope("ME", scope);
		RWFunctionDefinition rfn = rw.functions.get("ME.f");
		assertEquals("ME.f", rfn.name());
		assertTrue(rfn.cases.get(0).expr instanceof LocalVar);
		assertEquals("x", ((LocalVar)rfn.cases.get(0).expr).var);
	}

	@Test
	public void testWeRewriteStructFields() {
		StructDefn sd = new StructDefn(null, "Fred", true);
		sd.addField(new StructField(null, false, new TypeReference(null, "String"), "f"));
		scope.define("Container", "ME.Container", sd);
		rw.rewritePackageScope("ME", scope);
		RWStructDefn rsd = rw.structs.get("ME.Container");
		RWStructField sf = rsd.fields.get(0);
		assertEquals("f", sf.name);
		assertEquals("String", sf.type.name());
		assertEquals(WhatAmI.BUILTIN, sf.type.iam);
	}
	
	@Test
	public void testAStructReferencingAListFieldGetsARewrittenParameterList() {
		StructDefn sd = new StructDefn(posn, "Container", true);
		sd.addField(new StructField(posn, false, new TypeReference(posn, "List", new TypeReference(posn, "String")), "list"));
		scope.define("Container", "ME.Container", sd);
		rw.rewritePackageScope("ME", scope);
		RWStructDefn rsd = rw.structs.get("ME.Container");
		RWStructField sf = rsd.fields.get(0);
		assertEquals("list", sf.name);
		assertEquals("List", sf.type.name());
		assertEquals(WhatAmI.INSTANCE, sf.type.iam);
		assertTrue(sf.type.hasPolys());
		assertEquals(1, sf.type.polys().size());
		assertEquals("String", sf.type.poly(0).name());
		assertEquals(WhatAmI.BUILTIN, sf.type.poly(0).iam);
	}
	
	@Test
	public void testAStructReferencingAListFieldMustHaveATypeArgument() {
		StructDefn sd = new StructDefn(null, "Container", true);
		sd.addField(new StructField(null, false, new TypeReference(null, "List"), "list"));
		scope.define("Container", "ME.Container", sd);
		rw.rewritePackageScope("ME", scope);
		assertTrue(errors.hasErrors());
		assertEquals(1, errors.count());
		assertEquals("cannot use List without specifying polymorphic arguments", errors.get(0).msg);
	}
	
	@Test
	public void testRewritingANestedParameter() {
		Scope innerScope;
		{
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern(null, "x"));
			FunctionCaseDefn fn_f = new FunctionCaseDefn(null, CodeType.FUNCTION, "ME.f", args, new StringLiteral(null, "x"));
			scope.define("f", "ME.f", fn_f);
			innerScope = fn_f.innerScope();
		}
		{
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern(null, "y"));
			innerScope.define("g", "ME.f_0.g", new FunctionCaseDefn(null, CodeType.FUNCTION, "ME.f_0.g", args, new UnresolvedVar(null, "x")));
		}
		rw.rewritePackageScope("ME", scope);
		RWFunctionDefinition g = rw.functions.get("ME.f_0.g");
		System.out.println(rw.functions);
		assertEquals("ME.f_0.g", g.name());
		assertTrue(g.cases.get(0).expr instanceof LocalVar);
		assertEquals("x", ((LocalVar)g.cases.get(0).expr).var);
	}
	
	@Test
	public void testRewritingAStateVar() throws Exception {
		CardDefinition cd = new CardDefinition(null, null, scope, "MyCard");
		cd.state = new StateDefinition(posn);
		cd.state.fields.add(new StructField(null, false, new TypeReference(null, "Number"), "counter"));
//		scope.define("MyCard", "ME.MyCard", cd);
		cd.fnScope.define("f", "ME.MyCard.f", new FunctionCaseDefn(null, CodeType.FUNCTION, "ME.MyCard.f", new ArrayList<Object>(), new UnresolvedVar(null, "counter")));
		rw.rewritePackageScope("ME", scope);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		RWFunctionDefinition rfn = rw.functions.get("ME.MyCard.f");
		assertEquals("ME.MyCard.f", rfn.name());
		assertTrue(rfn.cases.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)rfn.cases.get(0).expr).var);
	}

	@Test
	public void testRewritingAContractVar() throws Exception {
		CardDefinition cd = new CardDefinition(null, null, scope, "MyCard");
		// TODO: I would have expected this to complain that it can't find the referenced contract
		cd.contracts.add(new ContractImplements(null, null, "Timer", null, "timer"));
//		scope.define("MyCard", "ME.MyCard", cd);
		cd.fnScope.define("f", "ME.MyCard.f", new FunctionCaseDefn(null, CodeType.FUNCTION, "ME.MyCard.f", new ArrayList<Object>(), new UnresolvedVar(null, "timer")));
		rw.rewritePackageScope("ME", scope);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		RWFunctionDefinition rfn = rw.functions.get("ME.MyCard.f");
		assertEquals("ME.MyCard.f", rfn.name());
		assertTrue(rfn.cases.get(0).expr instanceof CardMember);
		assertEquals("timer", ((CardMember)rfn.cases.get(0).expr).var);
	}

	@Test
	public void testRewritingAContractMethod() throws Exception {
		CardDefinition cd = new CardDefinition(null, null, scope, "MyCard");
		cd.state = new StateDefinition(posn);
		cd.state.fields.add(new StructField(null, false, new TypeReference(null, "Number"), "counter"));
		// TODO: I would have expected this to complain that it can't find the referenced contract
		ContractImplements ci = new ContractImplements(null, null, "Timer", null, "timer");
		cd.contracts.add(ci);
		MethodCaseDefn mcd1 = new MethodCaseDefn(new FunctionIntro(null, "ME.MyCard._C0.m", new ArrayList<Object>()));
		mcd1.messages.add(new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(null, "counter")), new UnresolvedVar(null, "counter")));
		ci.methods.add(mcd1);
//		scope.define("MyCard", "ME.MyCard", cd);
		rw.rewritePackageScope("ME", scope);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		RWMethodDefinition rmd = rw.methods.get(0).method;
		assertEquals("ME.MyCard._C0.m", rmd.name());
		assertTrue(rmd.cases.get(0).messages.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)rmd.cases.get(0).messages.get(0).expr).var);
	}

	// Handler with lambda

	@Test
	public void testRewritingAnEventHandler() throws Exception {
		CardDefinition cd = new CardDefinition(null, null, scope, "MyCard");
		cd.state = new StateDefinition(posn);
		cd.state.fields.add(new StructField(null, false, new TypeReference(null, "Number"), "counter"));
		// TODO: I would have expected this to complain that it can't find the referenced contract
		EventCaseDefn ecd1 = new EventCaseDefn(null, new FunctionIntro(null, "ME.MyCard.eh", new ArrayList<Object>()));
		ecd1.messages.add(new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(null, "counter")), new UnresolvedVar(null, "counter")));
		cd.fnScope.define("eh", "ME.MyCard.eh", ecd1);
//		scope.define("MyCard", "ME.MyCard", cd);
		rw.rewritePackageScope("ME", scope);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		RWEventHandlerDefinition reh = (RWEventHandlerDefinition) rw.eventHandlers.get(0).handler;
		assertEquals("ME.MyCard.eh", reh.name());
		assertTrue(reh.cases.get(0).messages.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)reh.cases.get(0).messages.get(0).expr).var);
	}
}
