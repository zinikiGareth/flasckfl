package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ContractMethodConsumer;
import org.flasck.flas.parser.ContractMethodParser;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ContractMethodMatcher;
import flas.matchers.VarPatternMatcher;

public class TDAContractMethodParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ContractMethodConsumer builder = context.mock(ContractMethodConsumer.class);
	private TopLevelDefinitionConsumer topLevel = context.mock(TopLevelDefinitionConsumer.class);
	private SolidName cname = new SolidName(new PackageName("test.repo"), "Contract");

	@Test
	public void aSimpleUpMethod() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.up("fred")));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder, topLevel, cname);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("up fred"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void aSimpleDownMethod() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.down("fred")));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder, topLevel, cname);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("down fred"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void sidewaysIsNotADirection() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(Tokenizable.class)), with("missing or invalid direction"));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder, topLevel, cname);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("sideways fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aDirectionIsRequired() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(Tokenizable.class)), with("missing or invalid direction"));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder, topLevel, cname);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void methodsMayBeDeclaredOptional() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.up("fred").optional()));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder, topLevel, cname);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("optional up fred"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void methodMayHaveOneSimpleArgument() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.up("fred").arg(VarPatternMatcher.var("test.repo.Contract.fred.x"))));
			oneOf(topLevel).argument(with(any(VarPattern.class)));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder, topLevel, cname);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("up fred x"));
		assertTrue(nested instanceof NoNestingParser);
	}
}
