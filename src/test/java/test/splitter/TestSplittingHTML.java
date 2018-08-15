package test.splitter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.flasck.flas.htmlzip.MultiSink;
import org.flasck.flas.htmlzip.Sink;
import org.flasck.flas.htmlzip.Splitter;
import org.flasck.flas.htmlzip.StdoutSink;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSplittingHTML {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();
	public Sink mock = context.mock(Sink.class);
	public Splitter splitter;

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			oneOf(mock).beginFile("text");
			oneOf(mock).fileEnd();
		}});
		Sink sink = new MultiSink(new StdoutSink(), mock);
		splitter = new Splitter(sink);
	}
	
	private void go(String input) throws IOException {
		splitter.extract("text", new ByteArrayInputStream(input.getBytes()));
	}
	
	@Test
	public void nothingHappensForAFileWithNoMagicAttributes() throws IOException {
		go("<div></div>");
	}
	
	@Test
	public void weCanExtractASimpleCard() throws IOException {
		context.checking(new Expectations() {{
			oneOf(mock).card("foo", 0, 37);
			oneOf(mock).dodgyAttr(5, 25);
		}});
		go("<div data-flas-card='foo'>hello</div>");
	}
	
	@Test
	public void aCardCanHaveAHole() throws IOException {
		context.checking(new Expectations() {{
			oneOf(mock).card("foo", 0, 69);
			oneOf(mock).dodgyAttr(5, 25);
			oneOf(mock).holeid("bar", 31, 51);
			oneOf(mock).hole(52, 57);
		}});
		go("<div data-flas-card='foo'><div data-flas-hole='bar'>hello</div></div>");
	}
	
	@Test
	public void aHoleCanInTurnHaveANestedCardDefinition() throws IOException {
		context.checking(new Expectations() {{
			oneOf(mock).card("foo", 0, 109);
			oneOf(mock).dodgyAttr(5, 25);
			oneOf(mock).holeid("bar", 31, 51);
			oneOf(mock).hole(52, 97);
			oneOf(mock).card("zeb", 56, 93);
			oneOf(mock).dodgyAttr(61, 81);
		}});
		go("<div data-flas-card='foo'><div data-flas-hole='bar'>junk<div data-flas-card='zeb'>hello</div>junk</div></div>");
	}
	
	@Test
	public void anyNodeCanDefineAnId() throws IOException {
		context.checking(new Expectations() {{
			oneOf(mock).card("foo", 0, 55);
			oneOf(mock).dodgyAttr(5, 25);
			oneOf(mock).identityAttr("mercy", 29, 39);
		}});
		go("<div data-flas-card='foo'><p id='mercy'>hello</p></div>");
	}
	
	@Test
	public void weCanRemoveOneRandomAttr() throws IOException {
		context.checking(new Expectations() {{
			oneOf(mock).card("foo", 0, 82);
			oneOf(mock).dodgyAttr(5, 25);
			oneOf(mock).dodgyAttr(29, 53);
			oneOf(mock).dodgyAttr(53, 67);
		}});
		go("<div data-flas-card='foo'><a data-flas-remove='href' href='foo.org'>link</a></div>");
	}
	
	@Test
	public void weCanRemoveMultipleRandomAttrs() throws IOException {
		context.checking(new Expectations() {{
			oneOf(mock).card("foo", 0, 102);
			oneOf(mock).dodgyAttr(5, 25);
			oneOf(mock).dodgyAttr(29, 59);
			oneOf(mock).dodgyAttr(59, 74);
			oneOf(mock).dodgyAttr(74, 87);
		}});
		go("<div data-flas-card='foo'><a data-flas-remove='href class' href='foo.org' class='alink'>link</a></div>");
	}
	
	@Test
	public void nothingBadHappensTryingToRemoveRandomAttrsThatArentThere() throws IOException {
		context.checking(new Expectations() {{
			oneOf(mock).card("foo", 0, 99);
			oneOf(mock).dodgyAttr(5, 25);
			oneOf(mock).dodgyAttr(29, 56);
		}});
		go("<div data-flas-card='foo'><a data-flas-remove='foo bar' href='foo.org' class='alink'>link</a></div>");
	}
}
