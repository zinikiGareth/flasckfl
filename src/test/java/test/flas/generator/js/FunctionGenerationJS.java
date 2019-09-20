package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSIfExpr;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIPatternTree;
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
		HSIPatternTree hsi = new HSIPatternTree(0);
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
		HSIPatternTree hsi = new HSIPatternTree(1);
		hsi.consider(fi);
		hsi.get(0).addCM("Nil", new HSIPatternTree(0).consider(fi));
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
		HSIPatternTree hsi = new HSIPatternTree(1);
		hsi.consider(f1);
		hsi.get(0).addCM("Nil", new HSIPatternTree(0).consider(f1));
		hsi.consider(f2);
		hsi.get(0).addCM("Cons", new HSIPatternTree(0).consider(f2));
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
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
			HSIPatternTree hsi = new HSIPatternTree(1);
			hsi.consider(fi);
			hsi.get(0).addCM("Nil", new HSIPatternTree(0).consider(fi));
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
			HSIPatternTree hsi = new HSIPatternTree(1);
			hsi.consider(fi);
			hsi.get(0).addCM("Nil", new HSIPatternTree(0).consider(fi));
			fn.bindHsi(hsi);
			new Traverser(gen).visitFunction(fn);
		}
	}
}
