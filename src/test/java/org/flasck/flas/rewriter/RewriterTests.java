package org.flasck.flas.rewriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.Rewriter.CardContext;
import org.flasck.flas.rewriter.Rewriter.PackageContext;
import org.flasck.flas.rewriter.Rewriter.RootContext;
import org.flasck.flas.rewriter.Rewriter.TemplateContext;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWEventHandlerDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.InstanceType;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.TypeWithName;
import org.junit.Before;
import org.junit.Test;

public class RewriterTests {
	private final InputPosition posn = new InputPosition("test", 1, 1, null);
	private final ErrorResult errors = new ErrorResult();
	private Rewriter rw;
	private final Scope scope = Scope.topScope("ME");
	
	@Before
	public void setup() {
		ImportPackage builtins = Builtin.builtins();
		builtins.define("Timer", new RWContractDecl(posn, posn, new SolidName(null, "Timer"), false));
		rw = new Rewriter(errors, null, builtins);
	}

	@Test
	public void testRewritingSomethingGloballyDefined() {
		FunctionCaseDefn fcd0 = new FunctionCaseDefn(FunctionName.function(posn, new PackageName("ME"), "f"), new ArrayList<Object>(), new UnresolvedVar(posn, "Nil"));
		fcd0.provideCaseName(0);
		scope.define(errors, "f", fcd0);
		rw.rewritePackageScope(null, null, "ME", scope);
		RWFunctionDefinition rfn = rw.functions.get("ME.f");
		assertEquals("ME.f", rfn.uniqueName());
		assertTrue(rfn.cases.get(0).expr instanceof PackageVar);
		assertEquals("Nil", ((PackageVar)rfn.cases.get(0).expr).id);
	}

	@Test
	public void testRewritingAParameter() {
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(new VarPattern(posn, "x"));
		FunctionCaseDefn fcd0 = new FunctionCaseDefn(FunctionName.function(posn, new PackageName("ME"), "f"), args, new UnresolvedVar(posn, "x"));
		fcd0.provideCaseName(0);
		scope.define(errors, "f", fcd0);
		rw.rewritePackageScope(null, null, "ME", scope);
		RWFunctionDefinition rfn = rw.functions.get("ME.f");
		assertEquals("ME.f", rfn.uniqueName());
		assertTrue(rfn.cases.get(0).expr instanceof LocalVar);
		assertEquals("x", ((LocalVar)rfn.cases.get(0).expr).var.var);
	}

	@Test
	public void testWeRewriteStructFields() {
		StructDefn sd = new StructDefn(posn, FieldsDefn.FieldsType.STRUCT, "ME", "Container", true);
		sd.addField(new StructField(posn, false, new TypeReference(posn, "String"), "f"));
		scope.define(errors, "Container", sd);
		rw.rewritePackageScope(null, null, "ME", scope);
		RWStructDefn rsd = (RWStructDefn) rw.getMe(posn, new SolidName(new PackageName("ME"), "Container")).defn;
		RWStructField sf = rsd.fields.get(0);
		assertEquals("f", sf.name);
		assertEquals("String", ((TypeWithName) sf.type).name());
		assertTrue(sf.type instanceof PrimitiveType);
	}
	
	@Test
	public void testAStructReferencingAListFieldGetsARewrittenParameterList() {
		StructDefn sd = new StructDefn(posn, FieldsDefn.FieldsType.STRUCT, "ME", "Container", true);
		sd.addField(new StructField(posn, false, new TypeReference(posn, "List", new TypeReference(posn, "String")), "list"));
		scope.define(errors, "Container", sd);
		rw.rewritePackageScope(null, null, "ME", scope);
		RWStructDefn rsd = (RWStructDefn) rw.getMe(posn, new SolidName(new PackageName("ME"), "Container")).defn;
		RWStructField sf = rsd.fields.get(0);
		assertEquals("list", sf.name);
		assertEquals("List", ((TypeWithName) sf.type).name());
		assertTrue(sf.type instanceof InstanceType);
		InstanceType st = (InstanceType)sf.type;
		assertTrue(st.hasPolys());
		assertEquals(1, st.polys().size());
		assertEquals("String", ((TypeWithName) st.poly(0)).name());
		assertTrue(st.poly(0) instanceof PrimitiveType);
	}
	
	@Test
	public void testAStructReferencingAListFieldMustHaveATypeArgument() {
		StructDefn sd = new StructDefn(posn, FieldsDefn.FieldsType.STRUCT, "ME", "Container", true);
		sd.addField(new StructField(null, false, new TypeReference(posn, "List"), "list"));
		scope.define(errors, "Container", sd);
		rw.rewritePackageScope(null, null, "ME", scope);
		assertTrue(errors.hasErrors());
		assertEquals(1, errors.count());
		assertEquals("cannot use List without specifying polymorphic arguments", errors.get(0).msg);
	}
	
	@Test
	public void testRewritingANestedParameter() {
		IScope innerScope;
		{
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern(posn, "x"));
			FunctionCaseDefn fn_f = new FunctionCaseDefn(FunctionName.function(posn, new PackageName("ME"), "f"), args, new StringLiteral(posn, "x"));
			fn_f.provideCaseName(0);
			scope.define(errors, "f", fn_f);
			innerScope = fn_f.innerScope();
		}
		{
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern(posn, "y"));
			FunctionCaseDefn fn_g = new FunctionCaseDefn(FunctionName.function(posn, new PackageName("ME.f_0"), "g"), args, new UnresolvedVar(posn, "x"));
			innerScope.define(errors, "g", fn_g);
			fn_g.provideCaseName(0);
		}
		rw.rewritePackageScope(null, null, "ME", scope);
		RWFunctionDefinition g = rw.functions.get("ME.f_0.g");
		assertEquals("ME.f_0.g", g.uniqueName());
		Object sv = g.cases.get(0).expr;
		assertTrue(sv instanceof ScopedVar);
		Object lv = ((ScopedVar)sv).defn;
		assertTrue(lv instanceof LocalVar);
		assertEquals("x", ((LocalVar)lv).var.var);
	}
	
	@Test
	public void testRewritingAStateVar() throws Exception {
		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, new CardName(new PackageName("ME"), "MyCard"));
		cd.state = new StateDefinition(posn);
		cd.state.fields.add(new StructField(posn, false, new TypeReference(posn, "Number"), "counter"));
//		scope.define(errors, "MyCard", "ME.MyCard", cd);
		FunctionCaseDefn fcd0 = new FunctionCaseDefn(FunctionName.function(posn, new PackageName("ME.MyCard"), "f"), new ArrayList<Object>(), new UnresolvedVar(null, "counter"));
		fcd0.provideCaseName(0);
		cd.fnScope.define(errors, "f", fcd0);
		rw.rewritePackageScope(null, null, "ME", scope);
		if (errors.hasErrors())
			errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		RWFunctionDefinition rfn = rw.functions.get("ME.MyCard.f");
		assertEquals("ME.MyCard.f", rfn.uniqueName());
		assertTrue(rfn.cases.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)rfn.cases.get(0).expr).var);
	}

	@Test
	public void testRewritingAContractVar() throws Exception {
		CardName cn = new CardName(new PackageName("ME"), "MyCard");
		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, cn);
		// TODO: I would have expected this to complain that it can't find the referenced contract
		ContractImplements ci = new ContractImplements(posn, posn, "Timer", posn, "timer");
		ci.setRealName(new CSName(cn, "_C0"));
		cd.contracts.add(ci);
//		scope.define(errors, "MyCard", "ME.MyCard", cd);
		FunctionCaseDefn fcd0 = new FunctionCaseDefn(FunctionName.function(posn, new PackageName("ME.MyCard"), "f"), new ArrayList<Object>(), new UnresolvedVar(null, "timer"));
		fcd0.provideCaseName(0);
		cd.fnScope.define(errors, "f", fcd0);
		rw.rewritePackageScope(null, null, "ME", scope);
		if (errors.hasErrors())
			errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		RWFunctionDefinition rfn = rw.functions.get("ME.MyCard.f");
		assertEquals("ME.MyCard.f", rfn.uniqueName());
		assertTrue(rfn.cases.get(0).expr instanceof CardMember);
		assertEquals("timer", ((CardMember)rfn.cases.get(0).expr).var);
	}

	@Test
	public void testRewritingAContractMethod() throws Exception {
		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, new CardName(new PackageName("ME"), "MyCard"));
		cd.state = new StateDefinition(posn);
		cd.state.fields.add(new StructField(posn, false, new TypeReference(posn, "Number"), "counter"));
		// TODO: I would have expected this to complain that it can't find the referenced contract
		CSName iName = new CSName(new CardName(new PackageName("ME"), "MyCard"), "_C0");
		ContractImplements ci = new ContractImplements(posn, posn, "Timer", posn, "timer");
		ci.setRealName(iName);
		cd.contracts.add(ci);
		MethodCaseDefn mcd1 = new MethodCaseDefn(new FunctionIntro(FunctionName.contractMethod(posn, iName, "m"), new ArrayList<Object>()));
		mcd1.provideCaseName(-1);
		mcd1.messages.add(new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "counter")), new UnresolvedVar(null, "counter")));
		ci.methods.add(mcd1);
//		scope.define(errors, "MyCard", "ME.MyCard", cd);
		rw.rewritePackageScope(null, null, "ME", scope);
		if (errors.hasErrors())
			errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		RWMethodDefinition rmd = rw.methods.get("ME.MyCard._C0.m");
		assertEquals("ME.MyCard._C0.m", rmd.name().uniqueName());
		assertTrue(rmd.cases.get(0).messages.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)rmd.cases.get(0).messages.get(0).expr).var);
	}

	// Handler with lambda

	@Test
	public void testRewritingAnEventHandler() throws Exception {
		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, new CardName(new PackageName("ME"), "MyCard"));
		cd.state = new StateDefinition(posn);
		cd.state.fields.add(new StructField(posn, false, new TypeReference(posn, "Number"), "counter"));
		// TODO: I would have expected this to complain that it can't find the referenced contract
		EventCaseDefn ecd1 = new EventCaseDefn(posn, new FunctionIntro(FunctionName.eventMethod(posn, new CardName(new PackageName("ME"), "MyCard"), "eh"), new ArrayList<Object>()));
		ecd1.provideCaseName(-1);
		ecd1.messages.add(new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "counter")), new UnresolvedVar(posn, "counter")));
		cd.fnScope.define(errors, "eh", ecd1);
//		scope.define(errors, "MyCard", "ME.MyCard", cd);
		rw.rewritePackageScope(null, null, "ME", scope);
		if (errors.hasErrors())
			errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		RWEventHandlerDefinition reh = (RWEventHandlerDefinition) rw.eventHandlers.get("ME.MyCard.eh");
		assertEquals("ME.MyCard.eh", reh.name().uniqueName());
		assertTrue(reh.cases.get(0).messages.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)reh.cases.get(0).messages.get(0).expr).var);
	}
	
	@Test
	public void testRewritingATemplateListVar() throws Exception {
		UnresolvedVar ur = new UnresolvedVar(posn, "e");
		ContentExpr ce = new ContentExpr(posn, ur, new ArrayList<>());
		final PackageName pn = new PackageName("foo");
		final CardName cn = new CardName(pn, "Card");
		RootContext rc = rw.new RootContext();
		PackageContext pc = rw.new PackageContext(rc, pn, scope);
		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, cn);
		CardContext cc = rw.new CardContext(pc, cn, null, cd, false);
		final AreaName an = new AreaName(cn, "area1");
		IterVar iv = new IterVar(posn, cn, "e");
		final TemplateListVar tlv = new TemplateListVar(posn, FunctionName.areaMethod(posn, an, "tlv_0"), iv);
		TemplateContext cx = rw.new TemplateContext(rw.new TemplateContext(cc), an, "e", tlv);
		RWContentExpr rwce = (RWContentExpr) rw.rewrite(cx, ce);
		if (errors.hasErrors())
			errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertNotNull(rwce);
		assertNotNull("expression was null", rwce.expr);
		assertTrue("expr was " + rwce.expr.getClass(), rwce.expr instanceof TemplateListVar);
	}

	@Test
	public void testRewritingATemplateListVarWithOneFieldExtracted() throws Exception {
		UnresolvedVar ur = new UnresolvedVar(posn, "e");
		ApplyExpr ae = new ApplyExpr(posn, new UnresolvedOperator(posn, "."), ur, new UnresolvedVar(posn, "id"));
		ContentExpr ce = new ContentExpr(posn, ae, new ArrayList<>());
		final PackageName pn = new PackageName("foo");
		final CardName cn = new CardName(pn, "Card");
		RootContext rc = rw.new RootContext();
		PackageContext pc = rw.new PackageContext(rc, pn, scope);
		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, cn);
		CardContext cc = rw.new CardContext(pc, cn, null, cd, false);
		final AreaName an = new AreaName(cn, "area1");
		IterVar iv = new IterVar(posn, cn, "e");
		final TemplateListVar tlv = new TemplateListVar(posn, FunctionName.areaMethod(posn, an, "tlv_0"), iv);
		TemplateContext cx = rw.new TemplateContext(rw.new TemplateContext(cc), an, "e", tlv);
		RWContentExpr rwce = (RWContentExpr) rw.rewrite(cx, ce);
		if (errors.hasErrors())
			errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertNotNull(rwce);
		assertNotNull("expression was null", rwce.expr);
		assertTrue("expr was " + rwce.expr.getClass(), rwce.expr instanceof ApplyExpr);
		assertTrue("inner expr was " + rwce.expr.getClass(), ((ApplyExpr)rwce.expr).args.get(0) instanceof TemplateListVar);
	}

	@Test
	public void testRewritingATemplateListVarWithNestedFieldExtracted() throws Exception {
		UnresolvedVar ur = new UnresolvedVar(posn, "e");
		ApplyExpr ae = new ApplyExpr(posn, new UnresolvedOperator(posn, "."), ur, new UnresolvedVar(posn, "id"));
		ApplyExpr ae2 = new ApplyExpr(posn, new UnresolvedOperator(posn, "."), ae, new UnresolvedVar(posn, "id"));
		ContentExpr ce = new ContentExpr(posn, ae2, new ArrayList<>());
		final PackageName pn = new PackageName("foo");
		final CardName cn = new CardName(pn, "Card");
		RootContext rc = rw.new RootContext();
		PackageContext pc = rw.new PackageContext(rc, pn, scope);
		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, cn);
		CardContext cc = rw.new CardContext(pc, cn, null, cd, false);
		final AreaName an = new AreaName(cn, "area1");
		IterVar iv = new IterVar(posn, cn, "e");
		final TemplateListVar tlv = new TemplateListVar(posn, FunctionName.areaMethod(posn, an, "tlv_0"), iv);
		TemplateContext cx = rw.new TemplateContext(rw.new TemplateContext(cc), an, "e", tlv);
		RWContentExpr rwce = (RWContentExpr) rw.rewrite(cx, ce);
		if (errors.hasErrors())
			errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertNotNull(rwce);
		assertNotNull("expression was null", rwce.expr);
		assertTrue("expr was " + rwce.expr.getClass(), rwce.expr instanceof ApplyExpr);
		assertTrue("inner expr was " + rwce.expr.getClass(), ((ApplyExpr)rwce.expr).args.get(0) instanceof ApplyExpr);
		assertTrue("double inner expr was " + rwce.expr.getClass(), ((ApplyExpr)(((ApplyExpr)rwce.expr).args.get(0))).args.get(0) instanceof TemplateListVar);
	}
//
//	@Test
//	public void testRewritingAWebzipDiv() throws Exception {
//		webzipBlocks.beginFile("foo");
//		webzipBlocks.card("card", 25, 95);
//		final PackageName pn = new PackageName("foo");
//		final CardName cn = new CardName(pn, "Card");
//		RootContext rc = rw.new RootContext();
//		PackageContext pc = rw.new PackageContext(rc, pn, scope);
//		CardDefinition cd = new CardDefinition(errors, posn, posn, scope, cn);
//		CardContext cc = rw.new CardContext(pc, cn, null, cd, false);
//		TemplateContext cx = rw.new TemplateContext(cc);
//		TemplateDiv div = new TemplateDiv(posn, "card", null, null, null, null, new ArrayList<>(), new ArrayList<>());
//		RWTemplateDiv rwdiv = (RWTemplateDiv) rw.rewrite(cx, div);
//		if (errors.hasErrors())
//			errors.showTo(new PrintWriter(System.out), 0);
//		assertFalse(errors.hasErrors());
//		assertNotNull(rwdiv);
//		assertTrue(rwdiv.webzip instanceof Block);
//	}
}
