package test.parsing.ut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.LocalErrorTracker;

public class TargetZoneParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private UnitTestNamer namer = context.mock(UnitTestNamer.class);
	private UnitTestDefinitionConsumer topLevel = context.mock(UnitTestDefinitionConsumer.class);
	private UnitTestStepConsumer builder = context.mock(UnitTestStepConsumer.class);
	private TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);

	@Test
	public void underscoreRepresentsTheWholeCard() {
		TargetZone tz = utp.parseTargetZone(UnitTestTopLevelParsingTests.line("_"));
		assertEquals(0, tz.length());
	}
	
	@Test
	public void weBackUpPastContains() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("_ contains");
		TargetZone tz = utp.parseTargetZone(line);
		assertEquals(0, tz.length());
		assertEquals(1, line.at());
	}
	
	@Test
	public void nothingIsNotAcceptable() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "valid target zone expected");
		}});
		TargetZone tz = utp.parseTargetZone(line);
		assertNull(tz);
	}
	
	@Test
	public void justContainsAssumesThatWeAreMatchingTheWholeCard() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("contains");
		TargetZone tz = utp.parseTargetZone(line);
		assertEquals(0, tz.length());
		assertEquals(0, line.at());
	}
	
	@Test
	public void weCanParseAField() {
		TargetZone tz = utp.parseTargetZone(UnitTestTopLevelParsingTests.line("x"));
		assertEquals(1, tz.length());
		assertEquals("x", tz.label(0));
	}
	
	@Test
	public void weCanParseAFieldWithContains() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("x contains");
		TargetZone tz = utp.parseTargetZone(line);
		assertEquals(1, tz.length());
		assertEquals("x", tz.label(0));
		assertEquals(2, line.at());
	}
	
	@Test
	public void weCanParseANestedField() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("x.y");
		TargetZone tz = utp.parseTargetZone(line);
		assertEquals(2, tz.length());
		assertEquals("x", tz.label(0));
		assertEquals("y", tz.label(1));
	}
	
	@Test
	public void weCanParseAListEntry() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("x.3.y");
		TargetZone tz = utp.parseTargetZone(line);
		assertEquals(3, tz.length());
		assertEquals("x", tz.label(0));
		assertEquals(3, tz.label(1));
		assertEquals("y", tz.label(2));
	}
	
	@Test
	public void weCannotHaveConsecutiveListEntries() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("x.3.1.y");
		InputPosition pos = line.locationAtText(4).copySetEnd(5);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot have consecutive list indices");
		}});
		TargetZone tz = utp.parseTargetZone(line);
		assertNull(tz);
	}
	
	@Test
	public void weCannotStartWithAListEntry() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("3.y");
		InputPosition pos = line.realinfo().copySetEnd(1);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "first entry in target cannot be list index");
		}});
		TargetZone tz = utp.parseTargetZone(line);
		assertNull(tz);
	}
	
	@Test
	public void weCannotStartWithADot() {
		Tokenizable line = UnitTestTopLevelParsingTests.line(".y");
		InputPosition pos = line.realinfo().copySetEnd(1);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "valid target zone expected");
		}});
		TargetZone tz = utp.parseTargetZone(line);
		assertNull(tz);
	}
	
	@Test
	public void weCannotHaveTwoDots() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("x..y");
		InputPosition pos = line.locationAtText(2).copySetEnd(3);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "valid target zone expected");
		}});
		TargetZone tz = utp.parseTargetZone(line);
		assertNull(tz);
	}

	@Test
	public void weCannotEndWithADot() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("x.");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "valid target zone expected");
		}});
		TargetZone tz = utp.parseTargetZone(line);
		assertNull(tz);
	}
	

	@Test
	public void containsAfterDotHasToBeAssumedToMeanAField() {
		Tokenizable line = UnitTestTopLevelParsingTests.line("x.contains");
		TargetZone tz = utp.parseTargetZone(line);
		assertEquals(2, tz.length());
		assertEquals("x", tz.label(0));
		assertEquals("contains", tz.label(1));
	}
}
