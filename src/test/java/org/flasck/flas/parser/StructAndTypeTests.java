package org.flasck.flas.parser;

import static org.junit.Assert.*;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class StructAndTypeTests {

	@Test
	public void testDefiningNil() {
		StructIntroParser p = new StructIntroParser();
		Object o = p.tryParsing(new Tokenizable(new StringBuilder("struct Nil")));
		assertNotNull(o);
		assertTrue(o instanceof StructDefn);
		StructDefn sd = (StructDefn)o;
		assertEquals("Nil", sd.typename);
		assertEquals(0, sd.args.size());
	}

	@Test
	public void testDefiningConsIntro() {
		StructIntroParser p = new StructIntroParser();
		Object o = p.tryParsing(new Tokenizable(new StringBuilder("struct Cons A")));
		assertNotNull(o);
		assertTrue(o instanceof StructDefn);
		StructDefn sd = (StructDefn)o;
		assertEquals("Cons", sd.typename);
		assertEquals(1, sd.args.size());
		assertEquals("A", sd.args.get(0));
	}

	@Test
	public void testDefiningConsHeadArg() {
		StructFieldParser p = new StructFieldParser();
		Object o = p.tryParsing(new Tokenizable(new StringBuilder("A head")));
		assertNotNull(o);
		assertTrue(o instanceof StructField);
		StructField sf = (StructField)o;
		assertTrue(sf.type instanceof TypeReference);
		TypeReference tr = (TypeReference) sf.type;
		assertEquals("A", tr.name);
		assertEquals("head", sf.name);
	}

	@Test
	public void testDefiningConsTailListA() {
		StructFieldParser p = new StructFieldParser();
		Object o = p.tryParsing(new Tokenizable(new StringBuilder("(List A) tail")));
		assertNotNull(o);
		assertTrue(o instanceof StructField);
		StructField sf = (StructField)o;
		assertTrue(sf.type instanceof TypeReference);
		TypeReference tr = (TypeReference) sf.type;
		assertEquals("List", tr.name);
		assertEquals(1, tr.args.size());
		assertTrue(tr.args.get(0) instanceof TypeReference);
		assertEquals("A", ((TypeReference)tr.args.get(0)).name);
		assertEquals("tail", sf.name);
	}


	@Test
	public void testDefiningTypeListA() {
		TypeDefnParser p = new TypeDefnParser();
		Object o = p.tryParsing(new Tokenizable(new StringBuilder("type List A = Nil | Cons A")));
		assertNotNull(o);
		assertTrue(o instanceof TypeDefn);
		TypeDefn sf = (TypeDefn)o;
		assertEquals("List", sf.defining.name);
		assertEquals(1, sf.defining.args.size());
		assertTrue(sf.defining.args.get(0) instanceof TypeReference);
		assertEquals("A", ((TypeReference)sf.defining.args.get(0)).name);
		assertEquals(2, sf.cases.size());
		TypeReference t0 = sf.cases.get(0);
		assertEquals("Nil", t0.name);
		assertEquals(0, t0.args.size());
		TypeReference t1 = sf.cases.get(1);
		assertEquals("Cons", t1.name);
		assertEquals(1, t1.args.size());
		Object ta = t1.args.get(0);
		assertTrue(ta instanceof TypeReference);
		assertEquals("A", ((TypeReference) ta).name);
	}

}
