package test.flas.generator.js;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jsgen.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSIfExpr;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
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
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FunctionGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);
	private final JSMethodCreator meth = context.mock(JSMethodCreator.class);

	@Test
	public void aSimpleFunction() {
		JSExpr nret = context.mock(JSExpr.class, "nret");
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt");
			oneOf(meth).literal("42"); will(returnValue(nret));
			oneOf(meth).returnObject(nret);
		}});
		JSGenerator gen = new JSGenerator(jss);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, new NumericLiteral(pos, "42", 2));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
	}
	
	@Test
	public void aMinimalHSIFunction() {
		JSExpr sret = context.mock(JSExpr.class, "sret");
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSExpr slot0 = context.mock(JSExpr.class, "slot0");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argument("_0"); will(returnValue(slot0));

			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Nil"); will(returnValue(nilSwitch));
			oneOf(isNil).string("hello"); will(returnValue(sret));
			oneOf(isNil).returnObject(sret);
			
			oneOf(notNil).errorNoCase();
		}});
		JSGenerator gen = new JSGenerator(jss);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(fi);
		hsi.get(0).requireCM("Nil").consider(fi);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
	}

	@Test
	public void functionArgsAreBoundToRealVars() {
		JSExpr ret = context.mock(JSExpr.class, "ret");
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSExpr slot0 = context.mock(JSExpr.class, "slot0");
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "f"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argument("_0"); will(returnValue(slot0));
			oneOf(meth).bindVar("_0", "x");
			oneOf(meth).boundVar("x"); will(returnValue(ret));
			oneOf(meth).returnObject(ret);
		}});
		FunctionName name = FunctionName.function(pos, pkg, "f");
		VarName vnx = new VarName(pos, name, "x");
		JSGenerator gen = new JSGenerator(jss);
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar ex = new UnresolvedVar(pos, "x");
		ex.bind(new VarPattern(pos, vnx));
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, ex);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(fi);
		hsi.get(0).addVar(vnx, fi);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
	}

	@Test
	public void argsAreBoundInsideBlocks() {
		JSExpr ret = context.mock(JSExpr.class, "ret");
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSExpr slot0 = context.mock(JSExpr.class, "slot0");
		JSExpr slot1 = context.mock(JSExpr.class, "slot1");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "f"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argument("_0"); will(returnValue(slot0));
			oneOf(meth).argument("_1"); will(returnValue(slot1));
			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Nil"); will(returnValue(nilSwitch));
			oneOf(isNil).bindVar("_0", "x");
			oneOf(isNil).boundVar("x"); will(returnValue(ret));
			oneOf(isNil).returnObject(ret);
			oneOf(notNil).errorNoCase();
		}});
		FunctionName name = FunctionName.function(pos, pkg, "f");
		VarName vnx = new VarName(pos, name, "x");
		JSGenerator gen = new JSGenerator(jss);
		FunctionDefinition fn = new FunctionDefinition(name, 2);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar ex = new UnresolvedVar(pos, "x");
		ex.bind(new VarPattern(pos, vnx));
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, ex);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(2);
		hsi.consider(fi);
		hsi.get(0).requireCM("Nil").consider(fi);
		hsi.get(1).addVar(vnx, fi);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
	}

	@Test
	public void aTwoConstructorHSIFunction() {
		JSExpr sret = context.mock(JSExpr.class, "sret");
		JSExpr gret = context.mock(JSExpr.class, "gret");
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSExpr slot0 = context.mock(JSExpr.class, "slot0");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSBlockCreator isCons = context.mock(JSBlockCreator.class, "isCons");
		JSBlockCreator notCons = context.mock(JSBlockCreator.class, "notCons");
		JSIfExpr consSwitch = new JSIfExpr(null, isCons, notCons);
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argument("_0"); will(returnValue(slot0));

			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Cons"); will(returnValue(consSwitch));
			oneOf(isCons).string("goodbye"); will(returnValue(gret));
			oneOf(isCons).returnObject(gret);
			oneOf(notCons).ifCtor("_0", "Nil"); will(returnValue(nilSwitch));
			oneOf(isNil).string("hello"); will(returnValue(sret));
			oneOf(isNil).returnObject(sret);
			
			oneOf(notNil).errorNoCase();
		}});
		JSGenerator gen = new JSGenerator(jss);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		FunctionIntro f1 = new FunctionIntro(name, new ArrayList<>());
		{
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			f1.functionCase(fcd);
			fn.intro(f1);
		}
		FunctionIntro f2 = new FunctionIntro(name, new ArrayList<>());
		{
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "goodbye"));
			f2.functionCase(fcd);
			fn.intro(f2);
		}
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(f1);
		hsi.get(0).requireCM("Nil").consider(f1);
		hsi.consider(f2);
		hsi.get(0).requireCM("Cons").consider(f2);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
	}

	@Test
	public void handleAField() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSGenerator gen = JSGenerator.forTests(meth, cxt);
		FunctionName name = FunctionName.function(pos, pkg, "x");

		JSBlockCreator isCons = context.mock(JSBlockCreator.class, "isCons");
		JSBlockCreator notCons = context.mock(JSBlockCreator.class, "notCons");
		JSIfExpr outer = new JSIfExpr(null, isCons, notCons);

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		intro.functionCase(new FunctionCaseDefn(null, expr));
		
		context.checking(new Expectations() {{
			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Cons"); will(returnValue(outer));
			oneOf(isCons).field("_1", "_0", "head");
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0));
		gen.switchOn(a0);
		gen.withConstructor("Cons");
		HSIPatternOptions headOpts = new HSIPatternOptions();
		headOpts.includes(intro);
		Slot cm1 = new CMSlot(headOpts);
		gen.constructorField(a0, "head", cm1);

		JSIfExpr inner = new JSIfExpr(null, context.mock(JSBlockCreator.class, "innerT"), context.mock(JSBlockCreator.class, "innerF"));
		context.checking(new Expectations() {{
			oneOf(isCons).head("_1");
			oneOf(isCons).ifCtor("_1", "True"); will(returnValue(inner));
		}});
		gen.switchOn(cm1);
		gen.withConstructor("True");
	}

	@Test
	public void constructorOrVar() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSExpr dummy = context.mock(JSExpr.class, "dummy");
		JSGenerator gen = JSGenerator.forTests(meth, cxt);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil, notNil);
		context.checking(new Expectations() {{
			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Nil"); will(returnValue(outer));
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0));
		gen.switchOn(a0);
		gen.withConstructor("Nil");

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		intro.functionCase(new FunctionCaseDefn(null, expr));

		context.checking(new Expectations() {{
			oneOf(isNil).string("hello"); will(returnValue(dummy));
		}});
		gen.startInline(intro);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.endInline(intro);
		
		final FunctionIntro intro2;
		intro2 = new FunctionIntro(name, new ArrayList<>());
		NumericLiteral number = new NumericLiteral(pos, "42", 2);
		intro2.functionCase(new FunctionCaseDefn(null, number));

		context.checking(new Expectations() {{
			oneOf(isNil).returnObject(dummy);
			oneOf(notNil).literal("42"); will(returnValue(dummy));
		}});
		gen.defaultCase();
		gen.startInline(intro2);
		gen.visitExpr(number, 0);
		gen.visitNumericLiteral(number);
		gen.endInline(intro2);

		context.checking(new Expectations() {{
			oneOf(notNil).returnObject(dummy);
		}});
		gen.endSwitch();
	}

	@Test
	public void numericConstants() {
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSExpr dummy = context.mock(JSExpr.class, "dummy");
		JSGenerator gen = JSGenerator.forTests(meth, cxt);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil, notNil);
		context.checking(new Expectations() {{
			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Number"); will(returnValue(outer));
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0));
		gen.switchOn(a0);
		gen.withConstructor("Number");

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isNil).ifConst("_0", 42); will(returnValue(inner));
		}});
		gen.matchNumber(42);

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		intro.functionCase(new FunctionCaseDefn(null, expr));

		context.checking(new Expectations() {{
			oneOf(isNil1).string("hello"); will(returnValue(dummy));
		}});
		gen.startInline(intro);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.endInline(intro);
		
		context.checking(new Expectations() {{
			oneOf(isNil1).returnObject(dummy);
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
		JSExpr dummy = context.mock(JSExpr.class, "dummy");
		JSGenerator gen = JSGenerator.forTests(meth, cxt);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil, notNil);
		context.checking(new Expectations() {{
			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "String"); will(returnValue(outer));
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0));
		gen.switchOn(a0);
		gen.withConstructor("String");

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isNil).ifConst("_0", "hello"); will(returnValue(inner));
		}});
		gen.matchString("hello");

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		intro.functionCase(new FunctionCaseDefn(null, expr));

		context.checking(new Expectations() {{
			oneOf(isNil1).string("hello"); will(returnValue(dummy));
		}});
		gen.startInline(intro);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.endInline(intro);
		
		context.checking(new Expectations() {{
			oneOf(isNil1).returnObject(dummy);
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
		JSExpr dummy = context.mock(JSExpr.class, "dummy");
		JSGenerator gen = JSGenerator.forTests(meth, cxt);
		
		JSBlockCreator isNil0 = context.mock(JSBlockCreator.class, "isNil0");
		JSBlockCreator notNil0 = context.mock(JSBlockCreator.class, "notNil0");
		JSIfExpr outer = new JSIfExpr(null, isNil0, notNil0);
		context.checking(new Expectations() {{
			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Nil"); will(returnValue(outer));
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		ArgSlot a1 = new ArgSlot(1, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0, a1));
		gen.switchOn(a0);
		gen.withConstructor("Nil");

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isNil0).head("_1");
			oneOf(isNil0).ifCtor("_1", "Nil"); will(returnValue(inner));
		}});
		gen.switchOn(a1);
		gen.withConstructor("Nil");
		
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		intro.functionCase(fcd);

		context.checking(new Expectations() {{
			oneOf(isNil1).string("hello"); will(returnValue(dummy));
		}});
		gen.startInline(intro);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.endInline(intro);
		
		context.checking(new Expectations() {{
			oneOf(isNil1).returnObject(dummy);
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
		JSExpr dummy = context.mock(JSExpr.class, "dummy");
		JSGenerator gen = JSGenerator.forTests(meth, cxt);
		
		JSBlockCreator isTrue0 = context.mock(JSBlockCreator.class, "isTrue0");
		JSBlockCreator notTrue0 = context.mock(JSBlockCreator.class, "notTrue0");
		JSIfExpr outer = new JSIfExpr(null, isTrue0, notTrue0);
		context.checking(new Expectations() {{
			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "True"); will(returnValue(outer));
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		ArgSlot a1 = new ArgSlot(1, new HSIPatternOptions());
		gen.hsiArgs(Arrays.asList(a0, a1));
		gen.switchOn(a0);
		gen.withConstructor("True");

		JSBlockCreator isNil1 = context.mock(JSBlockCreator.class, "isNil1");
		JSBlockCreator notNil1 = context.mock(JSBlockCreator.class, "notNil1");
		JSIfExpr inner = new JSIfExpr(null, isNil1, notNil1);
		context.checking(new Expectations() {{
			oneOf(isTrue0).head("_1");
			oneOf(isTrue0).ifCtor("_1", "Nil"); will(returnValue(inner));
		}});
		gen.switchOn(a1);
		gen.withConstructor("Nil");
		
		{
			FunctionIntro intro1 = new FunctionIntro(name, new ArrayList<>());
			StringLiteral expr1 = new StringLiteral(pos, "hello");
			FunctionCaseDefn fcd1 = new FunctionCaseDefn(null, expr1);
			intro1.functionCase(fcd1);

			context.checking(new Expectations() {{
				oneOf(isNil1).string("hello"); will(returnValue(dummy));
			}});

			gen.startInline(intro1);
			gen.visitExpr(expr1, 0);
			gen.visitStringLiteral(expr1);
			gen.endInline(intro1);
		}
		
		context.checking(new Expectations() {{
			oneOf(isNil1).returnObject(dummy);
			oneOf(notNil1).errorNoCase();
		}});
		gen.defaultCase();
		gen.errorNoCase();
		gen.endSwitch();

		JSBlockCreator isFalse0 = context.mock(JSBlockCreator.class, "isFalse0");
		JSBlockCreator notFalse0 = context.mock(JSBlockCreator.class, "notFalse0");
		JSIfExpr outer2 = new JSIfExpr(null, isFalse0, notFalse0);
		context.checking(new Expectations() {{
			oneOf(notTrue0).ifCtor("_0", "False"); will(returnValue(outer2));
		}});
		gen.withConstructor("False");

		JSBlockCreator isNil1b = context.mock(JSBlockCreator.class, "isNil1b");
		JSBlockCreator notNil1b = context.mock(JSBlockCreator.class, "notNil1b");
		JSIfExpr innerB = new JSIfExpr(null, isNil1b, notNil1b);
		context.checking(new Expectations() {{
			oneOf(isFalse0).head("_1");
			oneOf(isFalse0).ifCtor("_1", "Nil"); will(returnValue(innerB));
		}});
		gen.switchOn(a1);
		gen.withConstructor("Nil");

		{
			FunctionIntro intro2 = new FunctionIntro(name, new ArrayList<>());
			StringLiteral expr2 = new StringLiteral(pos, "goodbye");
			FunctionCaseDefn fcd2 = new FunctionCaseDefn(null, expr2);
			intro2.functionCase(fcd2);

			context.checking(new Expectations() {{
				oneOf(isNil1b).string("goodbye"); will(returnValue(dummy));
			}});

			gen.startInline(intro2);
			gen.visitExpr(expr2, 0);
			gen.visitStringLiteral(expr2);
			gen.endInline(intro2);
		}

		context.checking(new Expectations() {{
			oneOf(notNil1b).errorNoCase();
			oneOf(isNil1b).returnObject(dummy);
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
		JSExpr sret = context.mock(JSExpr.class, "sret");
		JSExpr cxt = context.mock(JSExpr.class, "cxt");
		JSExpr slot0 = context.mock(JSExpr.class, "slot0");
		JSBlockCreator isNil = context.mock(JSBlockCreator.class, "isNil");
		JSBlockCreator notNil = context.mock(JSBlockCreator.class, "notNil");
		JSIfExpr nilSwitch = new JSIfExpr(null, isNil, notNil);
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argument("_0"); will(returnValue(slot0));

			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Nil"); will(returnValue(nilSwitch));
			oneOf(isNil).string("hello"); will(returnValue(sret));
			oneOf(isNil).returnObject(sret);
			
			oneOf(notNil).errorNoCase();

			oneOf(jss).newFunction("test.repo", "y"); will(returnValue(meth));
			oneOf(meth).argument("_cxt"); will(returnValue(cxt));
			oneOf(meth).argument("_0"); will(returnValue(slot0));

			oneOf(meth).head("_0");
			oneOf(meth).ifCtor("_0", "Nil"); will(returnValue(nilSwitch));
			oneOf(isNil).string("hello"); will(returnValue(sret));
			oneOf(isNil).returnObject(sret);
			
			oneOf(notNil).errorNoCase();
		}});
		JSGenerator gen = new JSGenerator(jss);
		{
			FunctionName name = FunctionName.function(pos, pkg, "x");
			FunctionDefinition fn = new FunctionDefinition(name, 1);
			FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIArgsTree hsi = new HSIArgsTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM("Nil").consider(fi);
			fn.bindHsi(hsi);
			new Traverser(gen).visitFunction(fn);
		}
		{
			FunctionName name = FunctionName.function(pos, pkg, "y");
			FunctionDefinition fn = new FunctionDefinition(name, 1);
			FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIArgsTree hsi = new HSIArgsTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM("Nil").consider(fi);
			fn.bindHsi(hsi);
			new Traverser(gen).visitFunction(fn);
		}
	}
}
