package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSClassCreator;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ObjectGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);

	@Test
	public void aClassWithEvalIsAlwaysGenerated() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr obj = context.mock(JSExpr.class);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", "test.repo.Obj"); will(returnValue(clz));
			oneOf(clz).createMethod("eval", false); will(returnValue(meth));
			oneOf(meth).newOf(sn); will(returnValue(obj));
			oneOf(meth).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		new Traverser(gen).visitObjectDefn(od);
	}
}
