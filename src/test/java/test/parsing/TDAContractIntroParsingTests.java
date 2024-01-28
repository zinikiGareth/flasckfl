package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parser.ContractMethodParser;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAContractIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");
	
	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
	}

	@Test
	public void theSimplestContractDefinitionAcceptsANameAndReturnsAMethodParser() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(builder).newContract(with(errors), with(any(ContractDecl.class))); will(captureIt);
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("contract Data"));
		assertTrue(TDAParsingWithAction.is(nested, ContractMethodParser.class));
		ContractDecl cd = (ContractDecl) captureIt.get(1);
		assertEquals(ContractType.CONTRACT, cd.type);
	}

	@Test
	public void aSimpleServiceContract() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(builder).newContract(with(errors), with(any(ContractDecl.class))); will(captureIt);
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("contract service Data"));
		assertTrue(TDAParsingWithAction.is(nested, ContractMethodParser.class));
		ContractDecl cd = (ContractDecl) captureIt.get(1);
		assertEquals(ContractType.SERVICE, cd.type);
	}

	@Test
	public void aSimpleHandlerContract() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(builder).newContract(with(errors), with(any(ContractDecl.class))); will(captureIt);
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("contract handler Data"));
		assertTrue(TDAParsingWithAction.is(nested, ContractMethodParser.class));
		ContractDecl cd = (ContractDecl) captureIt.get(1);
		assertEquals(ContractType.HANDLER, cd.type);
	}

	@Test
	public void aContractMayHaveAMethod() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(builder).newContract(with(tracker), with(any(ContractDecl.class))); will(captureIt);
			oneOf(builder).newContractMethod(with(tracker), with(any(ContractMethodDecl.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("contract Data"));
		nested.tryParsing(TestSupport.tokline("foo"));
		nested.scopeComplete(new InputPosition("-", 10, 0, null, "hello"));
		ContractDecl cd = (ContractDecl) captureIt.get(1);
		assertEquals(1, cd.methods.size());
	}

	@Test
	public void aContractMayHaveMultipleMethods() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(builder).newContract(with(tracker), with(any(ContractDecl.class))); will(captureIt);
			exactly(2).of(builder).newContractMethod(with(tracker), with(any(ContractMethodDecl.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("contract Data"));
		nested.tryParsing(TestSupport.tokline("foo"));
		nested.tryParsing(TestSupport.tokline("bar"));
		nested.scopeComplete(new InputPosition("-", 10, 0, null, "hello"));
		ContractDecl cd = (ContractDecl) captureIt.get(1);
		assertEquals(2, cd.methods.size());
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TestSupport.tokline("contract");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TestSupport.tokline("contract fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(InputPosition.class)), with("invalid contract type"));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void junkMayNotFollowTheTypeName() {
		Tokenizable toks = TestSupport.tokline("contract Fred 42");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "tokens after end of line");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
}
