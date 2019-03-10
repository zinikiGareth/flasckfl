package test.builder;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ActualPhase2Processor;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ScopeBuilderTests implements ScopeReceiver {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private final PackageName pkg = new PackageName("test.building");
	private final InputPosition loc = new InputPosition("--", 3, 7, "posn");
	private final TopLevelDefnConsumer sb = new ActualPhase2Processor(errors, null, "test.building");
	private IScope s;

	@Before
	public void setup() {
		sb.scopeTo(this);
	}

	@Override
	public void provideScope(IScope scope) {
		this.s = scope;
	}
	
	@Test
	public void aStructCanBeAdded() {
		sb.newStruct(new StructDefn(loc, loc, FieldsDefn.FieldsType.STRUCT, new SolidName(pkg, "Hello"), true, new ArrayList<>()));
		assertEquals(1, s.size());
	}

	@Test
	public void aFunctionCaseDefnCanBeAdded() {
		sb.functionCase(new FunctionCaseDefn(FunctionName.function(loc, pkg, "f"), new ArrayList<>(), new StringLiteral(loc, "hello")));
		assertEquals(1, s.size());
	}
}
