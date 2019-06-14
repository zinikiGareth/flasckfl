package test.parsing;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.FunctionAssembler;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FunctionAssemblerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private FunctionScopeUnitConsumer consumer = context.mock(FunctionScopeUnitConsumer.class);
	private final PackageName pkg = new PackageName("test.pkg");
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void aSimpleIntroByItselfIsAssembled() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0).intros(1)));
		}});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer);
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "foo"), new ArrayList<>()));
		asm.moveOn();
	}

	@Test
	public void aScopeCanHaveTwoFunctions() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0).intros(1)));
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
		}});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer);
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "foo"), new ArrayList<>()));
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.moveOn();
	}
	
	@Test
	public void aScopeCanHaveTwoFunctionsSpreadAcrossMultipleIntros() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0)));
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(2)));
		}});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer);
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "foo"), new ArrayList<>()));
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.moveOn();
	}
	
	// This is basically a test that the repository will reject duplicate names, but it is saying that we don't treat it specially
	@Test(expected=DuplicateNameException.class)
	public void aScopeCannotHaveDefinitionsInARandomOrder() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0)));
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1))); will(throwException(new DuplicateNameException(pkg)));
		}});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer);
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "foo"), new ArrayList<>()));
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.moveOn();
	}

	// This depends on the "other things" telling the function assembler to "move on" ... we need to test that separately
	@Test(expected=DuplicateNameException.class)
	public void aScopeCannotHaveDefinitionsDividedByOtherElements() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
			oneOf(consumer).functionDefn(with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1))); will(throwException(new DuplicateNameException(pkg)));
		}});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer);
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.moveOn();
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.moveOn();
	}

	@Test
	public void aFunctionDefinitionCannotHaveDifferentNumbersOfFormalArgumentsInItsDifferentCases() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "inconsistent number of formal parameters");
		}});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer);
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList()));
		asm.functionIntro(new FunctionIntro(FunctionName.function(pos, pkg, "bar"), Arrays.asList(new Object())));
		asm.moveOn();
	}
}
