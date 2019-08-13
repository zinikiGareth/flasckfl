package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FunctionGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aSimpleFunction() {
		JSStorage jss = context.mock(JSStorage.class);
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr nret = context.mock(JSExpr.class, "nret");
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "x"); will(returnValue(meth));
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
		new Traverser(gen).visitFunction(fn);
	}
}
