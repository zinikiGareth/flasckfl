package test.parsing;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.FunctionAssembler;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

import flas.matchers.FunctionDefinitionMatcher;

public class FunctionAssemblerTests {
	@Rule
	public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private FunctionScopeUnitConsumer consumer = context.mock(FunctionScopeUnitConsumer.class);
	private final PackageName pkg = new PackageName("test.pkg");
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	Pattern p = new VarPattern(pos, new VarName(pos, pkg, "x"));

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
			allowing(errors).logReduction(with(any(String.class)), with(any(Locatable.class)), with(any(Locatable.class)));
		}});
	}

	@Test
	public void nothingHappensWithoutSomethingHappening() {
		FunctionAssembler asm = new FunctionAssembler(errors, consumer, null);
		asm.moveOn();
	}

	@Test
	public void aSimpleIntroByItselfIsAssembled() {
		context.checking(new Expectations() {
			{
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0).intros(1)));
			}
		});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer, null);
		asm.functionIntro(new FunctionIntro(caseName(asm, "foo"), new ArrayList<>()));
		asm.moveOn();
	}

	@Test
	public void aScopeCanHaveTwoFunctions() {
		context.checking(new Expectations() {
			{
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0).intros(1)));
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
			}
		});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer, null);
		asm.functionIntro(new FunctionIntro(caseName(asm, "foo"), new ArrayList<>()));
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.moveOn();
	}

	@Test
	public void aScopeCanHaveTwoFunctionsSpreadAcrossMultipleIntros() {
		context.checking(new Expectations() {
			{
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0)));
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(2)));
			}
		});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer, null);
		asm.functionIntro(new FunctionIntro(caseName(asm, "foo"), new ArrayList<>()));
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.moveOn();
	}

	// This is basically a test that the repository will reject duplicate names, but
	// it is saying that we don't treat it specially
	@Test
	public void aScopeCannotHaveDefinitionsInARandomOrder() {
		context.checking(new Expectations() {
			{
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.foo").args(0)));
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
			}
		});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer, null);
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.functionIntro(new FunctionIntro(caseName(asm, "foo"), new ArrayList<>()));
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.moveOn();
	}

	// This depends on the "other things" telling the function assembler to "move
	// on" ... we need to test that separately
	@Test
	public void aScopeCannotHaveDefinitionsDividedByOtherElements() {
		context.checking(new Expectations() {
			{
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
				oneOf(consumer).functionDefn(with(errors), with(FunctionDefinitionMatcher.named("test.pkg.bar").args(1).intros(1)));
			}
		});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer, null);
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.moveOn();
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.moveOn();
	}

	@Test
	public void aFunctionDefinitionCannotHaveDifferentNumbersOfFormalArgumentsInItsDifferentCases() {
		context.checking(new Expectations() {
			{
				oneOf(errors).message(pos, "inconsistent number of formal parameters");
			}
		});
		FunctionAssembler asm = new FunctionAssembler(errors, consumer, null);
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList()));
		asm.functionIntro(new FunctionIntro(caseName(asm, "bar"), Arrays.asList(p)));
		asm.moveOn();
	}

	FunctionName caseName(FunctionAssembler asm, String name) {
		FunctionName fname = FunctionName.function(pos, pkg, name);
		return FunctionName.caseName(fname, asm.nextCaseNumber(fname));
	}
}
