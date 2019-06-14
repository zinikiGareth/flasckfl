package test.parsing;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.FunctionAssembler;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FunctionAssemblerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ErrorReporter errors = new LocalErrorTracker(errorsMock);
	private FunctionScopeUnitConsumer consumer = context.mock(FunctionScopeUnitConsumer.class);
	private final PackageName pkg = new PackageName("test.pkg");
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void aSimpleIntroByItselfIsAssembled() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0)));
		}});
		FunctionAssembler asm = new FunctionAssembler(consumer);
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "foo"), new ArrayList<>()));
		asm.scopeDone();
	}

}
