package test.flas.generator.js;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jsgen.JSFunctionState;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.testsupport.matchers.SlotMatcher;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FunctionGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);
	private final JSFunctionState state = context.mock(JSFunctionState.class);
	private final JSMethodCreator meth = context.mock(JSMethodCreator.class);
	private final FunctionIntro intro = null;

	@Test
	public void aSimpleFunction() {
		JSExpr nret = context.mock(JSExpr.class, "nret");
		FunctionName name = FunctionName.function(pos, pkg, "x");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newFunction(name, new PackageName("test.repo"), new PackageName("test.repo"), false, "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt");
			oneOf(meth).argumentList();
			oneOf(meth).checkCached();
			oneOf(meth).cacheResult(with(any(JSExpr.class)));
			oneOf(meth).literal("42"); will(returnValue(nret));
			oneOf(meth).returnObject(nret);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		FunctionDefinition fn = new FunctionDefinition(name, 0, null);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, new NumericLiteral(pos, "42", 2));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		new Traverser(gen).withHSI().visitFunction(fn);
	}
	
	@Test
	public void aMinimalHSIFunction() {
		JSString sret = new JSString("s");
		JSExpr cxt = new JSVar("cxt");
		JSExpr slot0 = new JSVar("slot0");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		FunctionDefinition fn = new FunctionDefinition(name, 1, null);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, new StringLiteral(pos, "hello"));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(fi);
		hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi);
		fn.bindHsi(hsi);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newFunction(name, new PackageName("test.repo"), new PackageName("test.repo"), false, "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argumentList();
			oneOf(meth).argument("_0"); will(returnValue(slot0));

			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(nilSwitch));
			oneOf(isNil).string("hello"); will(returnValue(sret));
			oneOf(isNil).returnObject(sret);
			
			oneOf(notNil).errorNoCase();
		}});
		new Traverser(gen).withHSI().visitFunction(fn);
	}

	@Test
	public void functionArgsAreBoundToRealVars() {
		JSExpr ret = context.mock(JSExpr.class, "ret");
		JSExpr cxt = new JSVar("cxt");
		JSExpr slot0 = new JSVar("slot0");
		FunctionName name = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newFunction(name, new PackageName("test.repo"), new PackageName("test.repo"), false, "f"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argumentList();
			oneOf(meth).argument("_0"); will(returnValue(slot0));
			oneOf(meth).bindVar(with(SlotMatcher.id("0")), with(new JSVar("_0")), with("x"));
			oneOf(meth).boundVar("x"); will(returnValue(ret));
			oneOf(meth).returnObject(ret);
		}});
		VarName vnx = new VarName(pos, name, "x");
		VarPattern vp = new VarPattern(pos, vnx);
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		FunctionDefinition fn = new FunctionDefinition(name, 1, null);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar ex = new UnresolvedVar(pos, "x");
		ex.bind(new VarPattern(pos, vnx));
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, ex);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(fi);
		hsi.get(0).addVar(vp, fi);
		fn.bindHsi(hsi);
		new Traverser(gen).withHSI().visitFunction(fn);
	}

	@Test
	public void argsAreBoundInsideBlocks() {
		JSExpr ret = context.mock(JSExpr.class, "ret");
		JSExpr cxt = new JSVar("cxt");
		JSExpr slot0 = new JSVar("slot0");
		JSExpr slot1 = new JSVar("slot1");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		Sequence ordering = context.sequence("ordering");
		FunctionName name = FunctionName.function(pos, pkg, "f");
		VarName vnx = new VarName(pos, name, "x");
		VarPattern vp = new VarPattern(pos, vnx);
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		FunctionDefinition fn = new FunctionDefinition(name, 2, null);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar ex = new UnresolvedVar(pos, "x");
		ex.bind(new VarPattern(pos, vnx));
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, ex);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(2);
		hsi.consider(fi);
		hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi);
		hsi.get(1).addVar(vp, fi);
		fn.bindHsi(hsi);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newFunction(name, new PackageName("test.repo"), new PackageName("test.repo"), false, "f"); will(returnValue(meth)); inSequence(ordering);
			oneOf(meth).argument("_cxt"); will(returnValue(cxt)); inSequence(ordering);
			oneOf(meth).argumentList();
			oneOf(meth).argument("_0"); will(returnValue(slot0)); inSequence(ordering);
			oneOf(meth).argument("_1"); will(returnValue(slot1)); inSequence(ordering);
			oneOf(meth).head(new JSVar("_0")); inSequence(ordering);
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(nilSwitch)); inSequence(ordering);
			oneOf(isNil).bindVar(with(SlotMatcher.id("1")), with(new JSVar("_1")), with("x")); inSequence(ordering);
			oneOf(isNil).boundVar("x"); will(returnValue(ret)); inSequence(ordering);
			oneOf(isNil).returnObject(ret); inSequence(ordering);
			oneOf(notNil).errorNoCase();
		}});
		new Traverser(gen).withHSI().visitFunction(fn);
	}

	@Test
	public void aTwoConstructorHSIFunction() {
		JSString sret = new JSString("s");
		JSString gret = new JSString("g");
		JSExpr cxt = new JSVar("cxt");
		JSExpr slot0 = new JSVar("slot0");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSBlockCreator isCons = context.mock(JSBlockCreator.class, "isCons");
		JSBlockCreator notCons = context.mock(JSBlockCreator.class, "notCons");
		JSIfExpr consSwitch = new JSIfExpr(null, isCons, notCons);
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		FunctionDefinition fn = new FunctionDefinition(name, 1, null);
		FunctionIntro f1 = new FunctionIntro(name, new ArrayList<>());
		{
			FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, new StringLiteral(pos, "hello"));
			f1.functionCase(fcd);
			fn.intro(f1);
		}
		FunctionIntro f2 = new FunctionIntro(name, new ArrayList<>());
		{
			FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, new StringLiteral(pos, "goodbye"));
			f2.functionCase(fcd);
			fn.intro(f2);
		}
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(f1);
		hsi.get(0).requireCM(LoadBuiltins.nil).consider(f1);
		hsi.consider(f2);
		hsi.get(0).requireCM(LoadBuiltins.cons).consider(f2);
		fn.bindHsi(hsi);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newFunction(name, new PackageName("test.repo"), new PackageName("test.repo"), false, "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argumentList();
			oneOf(meth).argument("_0"); will(returnValue(slot0));

			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Cons")); will(returnValue(consSwitch));
			oneOf(isCons).string("goodbye"); will(returnValue(gret));
			oneOf(isCons).returnObject(gret);
			oneOf(notCons).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(nilSwitch));
			oneOf(isNil).string("hello"); will(returnValue(sret));
			oneOf(isNil).returnObject(sret);
			
			oneOf(notNil).errorNoCase();
		}});
		new Traverser(gen).withHSI().visitFunction(fn);
	}

	@Test
	public void handleAField() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		StackVisitor sv = new StackVisitor();
		JSGenerator.forTests(meth, cxt, sv);
		FunctionName name = FunctionName.function(pos, pkg, "x");

		JSBlockCreator isCons = context.mock(JSBlockCreator.class, "isCons");
		JSBlockCreator notCons = context.mock(JSBlockCreator.class, "notCons");
		JSIfExpr outer = new JSIfExpr(null, isCons, notCons);

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		intro.functionCase(new FunctionCaseDefn(pos, intro, null, expr));
		
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		context.checking(new Expectations() {{
			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Cons")); will(returnValue(outer));
		}});
		sv.hsiArgs(Arrays.asList(a0));
		sv.switchOn(a0);
		sv.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Cons"));
		HSIPatternOptions headOpts = new HSIPatternOptions();
		headOpts.includes(intro);
		Slot cm1 = new CMSlot("0_head", headOpts, null);
		context.checking(new Expectations() {{
			oneOf(isCons).field(new JSVar("_1"), new JSVar("_0"), "head");
		}});
		sv.constructorField(a0, "head", cm1);

		JSIfExpr inner = new JSIfExpr(null, context.mock(JSBlockCreator.class, "innerT"), context.mock(JSBlockCreator.class, "innerF"));
		context.checking(new Expectations() {{
			oneOf(isCons).head(new JSVar("_1"));
			oneOf(isCons).ifCtor(new JSVar("_1"), new SolidName(LoadBuiltins.builtinPkg, "True")); will(returnValue(inner));
		}});
		sv.switchOn(cm1);
		sv.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "True"));
	}

	@Test
	public void constructorOrVar() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSString dummy = new JSString("s");
		StackVisitor gen = new StackVisitor();
		JSGenerator.forTests(meth, cxt, gen, state);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil, notNil);
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		context.checking(new Expectations() {{
			allowing(state).ocret(); will(returnValue(null));
			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(outer));
		}});
		gen.hsiArgs(Arrays.asList(a0));
		gen.switchOn(a0);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(pos, intro, null, expr);
		intro.functionCase(fcd1);

		context.checking(new Expectations() {{
			oneOf(isNil).string("hello"); will(returnValue(dummy));
			oneOf(state).shouldCacheResult(); will(returnValue(false));
			oneOf(isNil).returnObject(dummy);
		}});
		gen.startInline(intro);
		gen.visitCase(fcd1);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.leaveCase(fcd1);
		gen.endInline(intro);
		
		final FunctionIntro intro2;
		intro2 = new FunctionIntro(name, new ArrayList<>());
		NumericLiteral number = new NumericLiteral(pos, "42", 2);
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(pos, intro, null, number);
		intro2.functionCase(fcd2);

		context.checking(new Expectations() {{
			oneOf(notNil).literal("42"); will(returnValue(dummy));
			oneOf(state).shouldCacheResult(); will(returnValue(false));
			oneOf(notNil).returnObject(dummy);
		}});
		gen.defaultCase();
		gen.startInline(intro2);
		gen.visitCase(fcd2);
		gen.visitExpr(number, 0);
		gen.visitNumericLiteral(number);
		gen.leaveCase(fcd2);
		gen.endInline(intro2);

		gen.endSwitch();
	}

	@Test
	public void numericConstants() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSString dummy = new JSString("s");
		StackVisitor gen = new StackVisitor();
		JSGenerator.forTests(meth, cxt, gen, state);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil, notNil);
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		context.checking(new Expectations() {{
			allowing(state).ocret(); will(returnValue(null));
			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Number")); will(returnValue(outer));
		}});
		gen.hsiArgs(Arrays.asList(a0));
		gen.switchOn(a0);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Number"));

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isNil).ifConst(new JSVar("_0"), 42); will(returnValue(inner));
		}});
		gen.matchNumber(42);

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, expr);
		intro.functionCase(fcd);

		context.checking(new Expectations() {{
			oneOf(isNil1).string("hello"); will(returnValue(dummy));
			oneOf(state).shouldCacheResult(); will(returnValue(false));
			oneOf(isNil1).returnObject(dummy);
		}});
		gen.startInline(intro);
		gen.visitCase(fcd);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.leaveCase(fcd);
		gen.endInline(intro);
		
		context.checking(new Expectations() {{
			oneOf(notNil1).errorNoCase();
			oneOf(notNil).errorNoCase();
		}});
		gen.matchDefault();
		gen.errorNoCase();
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();
	}

	@Test
	public void stringConstants() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSString dummy = new JSString("s");
		StackVisitor gen = new StackVisitor();
		JSGenerator.forTests(meth, cxt, gen, state);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil, notNil);
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		context.checking(new Expectations() {{
			allowing(state).ocret(); will(returnValue(null));
			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "String")); will(returnValue(outer));
		}});
		gen.hsiArgs(Arrays.asList(a0));
		gen.switchOn(a0);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "String"));

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isNil).ifConst(new JSVar("_0"), "hello"); will(returnValue(inner));
		}});
		gen.matchString("hello");

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, expr);
		intro.functionCase(fcd);

		context.checking(new Expectations() {{
			oneOf(isNil1).string("hello"); will(returnValue(dummy));
			oneOf(state).shouldCacheResult(); will(returnValue(false));
			oneOf(isNil1).returnObject(dummy);
		}});
		gen.startInline(intro);
		gen.visitCase(fcd);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.leaveCase(fcd);
		gen.endInline(intro);
		
		context.checking(new Expectations() {{
			oneOf(notNil1).errorNoCase();
			oneOf(notNil).errorNoCase();
		}});
		gen.matchDefault();
		gen.errorNoCase();
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();
	}

	@Test
	public void nestedSwitching() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSString dummy = new JSString("s");
		StackVisitor gen = new StackVisitor();
		JSGenerator.forTests(meth, cxt, gen, state);
		
		JSBlockCreator isNil0 = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil0 = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil0, notNil0);
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		context.checking(new Expectations() {{
			allowing(state).ocret(); will(returnValue(null));
			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(outer));
		}});
		ArgSlot a1 = new ArgSlot(1, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0, a1));
		gen.switchOn(a0);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isNil0).head(new JSVar("_1"));
			oneOf(isNil0).ifCtor(new JSVar("_1"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(inner));
		}});
		gen.switchOn(a1);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
		
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, expr);
		intro.functionCase(fcd);

		context.checking(new Expectations() {{
			oneOf(isNil1).string("hello"); will(returnValue(dummy));
			oneOf(state).shouldCacheResult(); will(returnValue(false));
			oneOf(isNil1).returnObject(dummy);
		}});
		gen.startInline(intro);
		gen.visitCase(fcd);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.leaveCase(fcd);
		gen.endInline(intro);
		
		context.checking(new Expectations() {{
			oneOf(notNil1).errorNoCase();
		}});
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();
		
		context.checking(new Expectations() {{
			oneOf(notNil0).errorNoCase();
		}});
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();
	}

	@Test
	public void nestedSwitchingWithTwoCases() {
		FunctionName name = FunctionName.function(pos, pkg, "c");
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSString dummy = new JSString("s");
		StackVisitor gen = new StackVisitor();
		JSGenerator.forTests(meth, cxt, gen, state);
		
		JSBlockCreator isTrue0 = context.mock(JSBlockCreator.class, "isTrue0");
		JSBlockCreator notTrue0 = context.mock(JSBlockCreator.class, "notTrue0");
		JSIfExpr outer = new JSIfExpr(null, isTrue0, notTrue0);
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		context.checking(new Expectations() {{
			allowing(state).ocret(); will(returnValue(null));
			oneOf(meth).head(new JSVar("_0"));
			oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "True")); will(returnValue(outer));
		}});
		ArgSlot a1 = new ArgSlot(1, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0, a1));
		gen.switchOn(a0);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "True"));

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isTrue0).head(new JSVar("_1"));
			oneOf(isTrue0).ifCtor(new JSVar("_1"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(inner));
		}});
		gen.switchOn(a1);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));
		
		{
			FunctionIntro intro1 = new FunctionIntro(name, new ArrayList<>());
			StringLiteral expr1 = new StringLiteral(pos, "hello");
			FunctionCaseDefn fcd1 = new FunctionCaseDefn(pos, intro, null, expr1);
			intro1.functionCase(fcd1);

			context.checking(new Expectations() {{
				oneOf(isNil1).string("hello"); will(returnValue(dummy));
				allowing(state).shouldCacheResult(); will(returnValue(false));
				oneOf(isNil1).returnObject(dummy);
			}});

			gen.startInline(intro1);
			gen.visitCase(fcd1);
			gen.visitExpr(expr1, 0);
			gen.visitStringLiteral(expr1);
			gen.leaveCase(fcd1);
			gen.endInline(intro1);
		}
		
		context.checking(new Expectations() {{
			oneOf(notNil1).errorNoCase();
		}});
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();

		JSBlockCreator isFalse0 = context.mock(JSBlockCreator.class, "isFalse0");
		JSBlockCreator notFalse0 = context.mock(JSBlockCreator.class, "notFalse0");
		JSIfExpr outer2 = new JSIfExpr(null, isFalse0, notFalse0);
		context.checking(new Expectations() {{
			oneOf(notTrue0).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "False")); will(returnValue(outer2));
		}});
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "False"));

		JSBlockCreator isNil1b = context.mock(JSBlockCreator.class, "isNil1b");
		JSBlockCreator notNil1b = context.mock(JSBlockCreator.class, "notNil1b");
		JSIfExpr innerB = new JSIfExpr(null, isNil1b, notNil1b);
		context.checking(new Expectations() {{
			oneOf(isFalse0).head(new JSVar("_1"));
			oneOf(isFalse0).ifCtor(new JSVar("_1"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(innerB));
		}});
		gen.switchOn(a1);
		gen.withConstructor(new SolidName(LoadBuiltins.builtinPkg, "Nil"));

		{
			FunctionIntro intro2 = new FunctionIntro(name, new ArrayList<>());
			StringLiteral expr2 = new StringLiteral(pos, "goodbye");
			FunctionCaseDefn fcd2 = new FunctionCaseDefn(pos, intro, null, expr2);
			intro2.functionCase(fcd2);

			context.checking(new Expectations() {{
				oneOf(isNil1b).string("goodbye"); will(returnValue(dummy));
				oneOf(isNil1b).returnObject(dummy);
			}});

			gen.startInline(intro2);
			gen.visitCase(fcd2);
			gen.visitExpr(expr2, 0);
			gen.visitStringLiteral(expr2);
			gen.leaveCase(fcd2);
			gen.endInline(intro2);
		}

		context.checking(new Expectations() {{
			oneOf(notNil1b).errorNoCase();
		}});
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();

		context.checking(new Expectations() {{
			oneOf(notFalse0).errorNoCase();
		}});
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();
	}

	@Test
	public void stateIsCleanedUpBetweenFunctions() {
		JSString sret = new JSString("s");
		JSExpr cxt = new JSVar("cxt");
		JSExpr slot0 = new JSVar("slot0");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		FunctionName nameY = FunctionName.function(pos, pkg, "y");
		
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		Traverser trav = new Traverser(gen).withHSI();
		{
			FunctionDefinition fn = new FunctionDefinition(nameX, 1, null);
			FunctionIntro fi = new FunctionIntro(nameX, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIArgsTree hsi = new HSIArgsTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi);
			fn.bindHsi(hsi);
			context.checking(new Expectations() {{
				oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
				oneOf(jss).newFunction(nameX, new PackageName("test.repo"), new PackageName("test.repo"), false, "x"); will(returnValue(meth));
				oneOf(meth).argument("_cxt"); will(returnValue(cxt));
				oneOf(meth).argumentList();
				oneOf(meth).argument("_0"); will(returnValue(slot0));

				oneOf(meth).head(new JSVar("_0"));
				oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(nilSwitch));
				oneOf(isNil).string("hello"); will(returnValue(sret));
				oneOf(isNil).returnObject(sret);
				
				oneOf(notNil).errorNoCase();
			}});
			trav.visitFunction(fn);
		}
		context.assertIsSatisfied();
		{
			FunctionDefinition fn = new FunctionDefinition(nameY, 1, null);
			FunctionIntro fi = new FunctionIntro(nameY, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIArgsTree hsi = new HSIArgsTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi);
			fn.bindHsi(hsi);
			context.checking(new Expectations() {{
				oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
				oneOf(jss).newFunction(nameY, new PackageName("test.repo"), new PackageName("test.repo"), false, "y"); will(returnValue(meth));
				oneOf(meth).argument("_cxt"); will(returnValue(cxt));
				oneOf(meth).argumentList();
				oneOf(meth).argument("_0"); will(returnValue(slot0));

				oneOf(meth).head(new JSVar("_0"));
				oneOf(meth).ifCtor(new JSVar("_0"), new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(nilSwitch));
				oneOf(isNil).string("hello"); will(returnValue(sret));
				oneOf(isNil).returnObject(sret);
				
				oneOf(notNil).errorNoCase();
			}});
			trav.visitFunction(fn);
		}
	}
}
