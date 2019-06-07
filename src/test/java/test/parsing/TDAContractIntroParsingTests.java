package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parser.ContractMethodParser;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

public class TDAContractIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(builder).scopeTo(with(any(ScopeReceiver.class)));
		}});
	}

	@Test
	public void theSimplestContractDefinitionAcceptsANameAndReturnsAMethodParser() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Data"); will(returnValue(new SolidName(null, "Data")));
			oneOf(builder).newContract(with(any(ContractDecl.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("contract Data"));
		assertTrue(nested instanceof ContractMethodParser);
	}

	@Test
	public void aContractMayHaveAMethod() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Data"); will(returnValue(new SolidName(null, "Data")));
			oneOf(builder).newContract(with(any(ContractDecl.class))); will(captureIt);
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("contract Data"));
		nested.tryParsing(TDABasicIntroParsingTests.line("up foo"));
		nested.scopeComplete(new InputPosition("-", 10, 0, "hello"));
		ContractDecl cd = (ContractDecl) captureIt.get(0);
		assertEquals(1, cd.methods.size());
	}

	@Test
	public void aContractMayHaveMultipleMethods() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Data"); will(returnValue(new SolidName(null, "Data")));
			oneOf(builder).newContract(with(any(ContractDecl.class))); will(captureIt);
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("contract Data"));
		nested.tryParsing(TDABasicIntroParsingTests.line("up foo"));
		nested.tryParsing(TDABasicIntroParsingTests.line("down bar"));
		nested.scopeComplete(new InputPosition("-", 10, 0, "hello"));
		ContractDecl cd = (ContractDecl) captureIt.get(0);
		assertEquals(2, cd.methods.size());
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TDABasicIntroParsingTests.line("contract");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TDABasicIntroParsingTests.line("contract fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void junkMayNotFollowTheTypeName() {
		Tokenizable toks = TDABasicIntroParsingTests.line("contract Fred 42");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "tokens after end of line");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
}