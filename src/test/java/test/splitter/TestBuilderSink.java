package test.splitter;

import org.flasck.flas.htmlzip.BuilderSink;
import org.flasck.flas.htmlzip.CardVisitor;
import org.flasck.flas.htmlzip.SplitterException;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class TestBuilderSink {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();
	public CardVisitor mock = context.mock(CardVisitor.class);

	@Test(expected=SplitterException.class)
	public void aCardCanOnlyBeCreatedInAnActiveFile() {
		BuilderSink sink = new BuilderSink();
		sink.card("hello", 25, 55);
	}

	@Test(expected=SplitterException.class)
	public void aCardCanOnlyBeCreatedInAnActiveFileEvenIfThereHasBeenAPreviouslyActiveFile() {
		BuilderSink sink = new BuilderSink();
		sink.beginFile("foo");
		sink.fileEnd();
		sink.card("hello", 25, 55);
	}

	@Test(expected=SplitterException.class)
	public void aCardCannotBeVisitedIfItWasNotDefined() {
		BuilderSink sink = new BuilderSink();
		sink.visitCard("hello", mock);
	}

	@Test
	public void aCardCanBeCreatedInAnActiveFile() {
		Sequence order = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(mock).consider("foo"); inSequence(order);
			oneOf(mock).render(25, 55); inSequence(order);
			oneOf(mock).done(); inSequence(order);
		}});
		BuilderSink sink = new BuilderSink();
		sink.beginFile("foo");
		sink.card("hello", 25, 55);
		sink.fileEnd();
		sink.visitCard("hello", mock);
	}

	@Test
	@Ignore
	public void aCardWithAHoleCanBeCreatedInAnActiveFile() {
		Sequence order = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(mock).consider("foo"); inSequence(order);
			oneOf(mock).render(25, 31); inSequence(order);
			oneOf(mock).renderIntoHole("bar"); inSequence(order);
			oneOf(mock).render(50, 55); inSequence(order);
			oneOf(mock).done(); inSequence(order);
		}});
		BuilderSink sink = new BuilderSink();
		sink.beginFile("foo");
		sink.card("hello", 25, 55);
		sink.hole("bar", 31, 50);
		sink.fileEnd();
		sink.dump();
		sink.visitCard("hello", mock);
	}

	@Test
	public void anIdCanBeIdentifiedAndInserted() {
		Sequence order = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(mock).consider("foo"); inSequence(order);
			oneOf(mock).render(25, 37); inSequence(order);
			oneOf(mock).id("div_1");
			oneOf(mock).render(42, 105); inSequence(order);
			oneOf(mock).done(); inSequence(order);
		}});
		BuilderSink sink = new BuilderSink();
		sink.beginFile("foo");
		sink.card("hello", 25, 105);
		sink.identityAttr("div_1", 37, 42);
		sink.fileEnd();
		sink.dump();
		sink.visitCard("hello", mock);
	}

	@Test
	public void randomAttrsCanBeRemoved() {
		Sequence order = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(mock).consider("foo"); inSequence(order);
			oneOf(mock).render(25, 37); inSequence(order);
			oneOf(mock).render(42, 105); inSequence(order);
			oneOf(mock).done(); inSequence(order);
		}});
		BuilderSink sink = new BuilderSink();
		sink.beginFile("foo");
		sink.card("hello", 25, 105);
		sink.dodgyAttr(37, 42);
		sink.fileEnd();
		sink.dump();
		sink.visitCard("hello", mock);
	}

	@Test
	public void adjacentAttrRemovalsAreCoalesced() {
		Sequence order = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(mock).consider("foo"); inSequence(order);
			oneOf(mock).render(25, 37); inSequence(order);
			oneOf(mock).render(49, 105); inSequence(order);
			oneOf(mock).done(); inSequence(order);
		}});
		BuilderSink sink = new BuilderSink();
		sink.beginFile("foo");
		sink.card("hello", 25, 105);
		sink.dodgyAttr(37, 42);
		sink.dodgyAttr(42, 49);
		sink.fileEnd();
		sink.dump();
		sink.visitCard("hello", mock);
	}

	@Test
	@Ignore
	public void adjacentHolesAndAttrRemovalsAreCoalesced() {
		Sequence order = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(mock).consider("foo"); inSequence(order);
			oneOf(mock).render(25, 37); inSequence(order);
			oneOf(mock).renderIntoHole("hole");
			oneOf(mock).render(49, 105); inSequence(order);
			oneOf(mock).done(); inSequence(order);
		}});
		BuilderSink sink = new BuilderSink();
		sink.beginFile("foo");
		sink.card("hello", 25, 105);
		sink.hole("hole", 37, 42);
		sink.dodgyAttr(42, 49);
		sink.fileEnd();
		sink.dump();
		sink.visitCard("hello", mock);
	}
}
