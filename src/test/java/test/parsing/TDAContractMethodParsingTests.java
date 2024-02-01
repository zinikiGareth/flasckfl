package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ContractMethodConsumer;
import org.flasck.flas.parser.ContractMethodParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.ContractMethodMatcher;
import org.flasck.flas.testsupport.matchers.TypedPatternMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAContractMethodParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ContractMethodConsumer builder = context.mock(ContractMethodConsumer.class);
	private TopLevelDefinitionConsumer topLevel = context.mock(TopLevelDefinitionConsumer.class);
	private SolidName cname = new SolidName(new PackageName("test.repo"), "Contract");

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
	}

	@Test
	public void aSimpleMethod() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.named("fred")));
			oneOf(topLevel).newContractMethod(with(errors), with(any(ContractMethodDecl.class)));
		}});
		Tokenizable line = TestSupport.tokline("fred");
		ContractMethodParser parser = new ContractMethodParser(errors, line.realinfo(), builder, topLevel, cname, null);
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void methodsMayNotHaveTypeNames() {
		ErrorMark mark = context.mock(ErrorMark.class);
		Tokenizable toks = TestSupport.tokline("Fred");
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(errors).message(with(any(InputPosition.class)), with("invalid method name"));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, toks.realinfo(), builder, topLevel, cname, null);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void aMethodNameMayBeCamelCase() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.named("fredBloggs")));
			oneOf(topLevel).newContractMethod(with(errors), with(any(ContractMethodDecl.class)));
		}});
		Tokenizable toks = TestSupport.tokline("fredBloggs");
		ContractMethodParser parser = new ContractMethodParser(errors, toks.realinfo(), builder, topLevel, cname, null);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void methodsMayBeDeclaredOptional() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.named("fred").optional()));
			oneOf(topLevel).newContractMethod(with(errors), with(any(ContractMethodDecl.class)));
		}});
		Tokenizable toks = TestSupport.tokline("optional fred");
		ContractMethodParser parser = new ContractMethodParser(errors, toks.realinfo(), builder, topLevel, cname, null);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void methodMayHaveOneTypedArgument() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.named("fred").arg(TypedPatternMatcher.typed("Number", "x"))));
			oneOf(topLevel).newContractMethod(with(errors), with(any(ContractMethodDecl.class)));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
		}});
		Tokenizable line = TestSupport.tokline("fred (Number x)");
		ContractMethodParser parser = new ContractMethodParser(errors, line.realinfo(), builder, topLevel, cname, null);
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void methodMayHaveAHandler() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.named("fred").handler(TypedPatternMatcher.typed("Handler", "h"))));
			oneOf(topLevel).argument(with(errors), (TypedPattern)with(TypedPatternMatcher.typed("Handler", "h")));
			oneOf(topLevel).newContractMethod(with(errors), with(any(ContractMethodDecl.class)));
		}});
		Tokenizable line = TestSupport.tokline("fred -> (Handler h)");
		ContractMethodParser parser = new ContractMethodParser(errors, line.realinfo(), builder, topLevel, cname, null);
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void methodMayNotHaveSimpleArguments() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			// it constructs the "wrong" thing before complaining
			oneOf(builder).addMethod(with(ContractMethodMatcher.named("fred")));
			oneOf(topLevel).newContractMethod(with(errors), with(any(ContractMethodDecl.class)));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
			// but it does complain and that should stop it doing the wrong thing later ...
			oneOf(errors).message(with(any(InputPosition.class)), with("contract patterns must be typed"));
		}});
		Tokenizable toks = TestSupport.tokline("fred x");
		ContractMethodParser parser = new ContractMethodParser(errors, toks.realinfo(), builder, topLevel, cname, null);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof NoNestingParser);
	}
}
