package org.flasck.flas.parser;

import static org.junit.Assert.*;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.typechecker.Type;
import org.junit.Test;

public class StructAndTypeTests {

	@Test
	public void testDefiningNil() {
		IntroParser p = new IntroParser(new State(null, null, null));
		Object o = p.tryParsing(new Tokenizable("struct Nil"));
		assertNotNull(o);
		assertTrue(o instanceof StructDefn);
		StructDefn sd = (StructDefn)o;
		assertEquals("Nil", sd.name());
		assertEquals(0, sd.polys().size());
	}

	@Test
	public void testDefiningConsIntro() {
		IntroParser p = new IntroParser(new State(null, null, null));
		Object o = p.tryParsing(new Tokenizable("struct Cons A"));
		assertNotNull(o);
		assertTrue(o instanceof StructDefn);
		StructDefn sd = (StructDefn)o;
		assertEquals("Cons", sd.name());
		assertEquals(1, sd.polys().size());
		assertEquals("A", sd.poly(0).name());
	}

	@Test
	public void testDefiningConsHeadArg() {
		FieldParser p = new FieldParser(FieldParser.CARD);
		Object o = p.tryParsing(new Tokenizable("A head"));
		assertNotNull(o);
		assertTrue(o instanceof StructField);
		StructField sf = (StructField)o;
		assertEquals("A", sf.type.name());
		assertEquals("head", sf.name);
	}

	@Test
	public void testDefiningConsTailListA() {
		FieldParser p = new FieldParser(FieldParser.CARD);
		Object o = p.tryParsing(new Tokenizable("(List A) tail"));
		assertNotNull(o);
		assertTrue(o instanceof StructField);
		StructField sf = (StructField)o;
		assertTrue(sf.type instanceof Type);
		Type tr = (Type) sf.type;
		assertEquals("List", tr.name());
		assertEquals(1, tr.polys().size());
		assertEquals("A", tr.poly(0).name());
		assertEquals("tail", sf.name);
	}


	@Test
	public void testDefiningTypeListA() {
		TypeDefnParser p = new TypeDefnParser();
		Object o = p.tryParsing(new Tokenizable("type List A = Nil | Cons A"));
		assertNotNull(o);
		assertTrue(o instanceof UnionTypeDefn);
		UnionTypeDefn sf = (UnionTypeDefn)o;
		assertEquals("List", sf.name());
		assertEquals(1, sf.polys().size());
		assertEquals("A", sf.poly(0).name());
		assertEquals(2, sf.cases.size());
		Type t0 = sf.cases.get(0);
		assertEquals("Nil", t0.name());
		assertEquals(0, t0.polys().size());
		Type t1 = sf.cases.get(1);
		assertEquals("Cons", t1.name());
		assertEquals(1, t1.polys().size());
		assertEquals("A", t1.poly(0).name());
	}

}
