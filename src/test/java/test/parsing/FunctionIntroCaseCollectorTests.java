package test.parsing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FunctionIntroCaseCollectorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final PackageName pkg = new PackageName("test.pkg");
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");

	@Test
	public void aSimpleIntroByItselfIsAssembled() {
		FunctionIntro intro = new FunctionIntro(FunctionName.function(pos, pkg, "foo"), new ArrayList<>());
		intro.functionCase(new FunctionCaseDefn(null, new StringLiteral(pos, "hello")));
		assertEquals(1, intro.cases().size());
	}
	
	// Do we need any more cases than this?
}
