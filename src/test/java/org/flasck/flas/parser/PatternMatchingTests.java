package org.flasck.flas.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.TuplePattern;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class PatternMatchingTests {

	@Test
	public void testSimpleVar() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("x"));
		assertNotNull(patt);
		assertTrue(patt instanceof VarPattern);
		assertEquals("x", ((VarPattern)patt).var);
	}

	@Test
	public void testTypedVar() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("(List l)"));
		assertNotNull(patt);
		assertTrue(patt instanceof TypedPattern);
		assertEquals("List", ((TypedPattern)patt).type.name());
		assertEquals("l", ((TypedPattern)patt).var);
	}

	@Test
	public void testTypedPolyVar() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("(List[String] l)"));
		assertNotNull(patt);
		assertTrue(patt instanceof TypedPattern);
		TypeReference ty = ((TypedPattern)patt).type;
		assertEquals("List", ty.name());
		assertTrue(ty.hasPolys());
		assertEquals(1, ty.polys().size());
		assertEquals("String", ty.polys().get(0).name());
		assertEquals("l", ((TypedPattern)patt).var);
	}

	@Test
	public void testTypedPolyVar2() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("(Dict[String,Number] l)"));
		assertNotNull(patt);
		assertTrue(patt instanceof TypedPattern);
		TypeReference ty = ((TypedPattern)patt).type;
		assertEquals("Dict", ty.name());
		assertTrue(ty.hasPolys());
		assertEquals(2, ty.polys().size());
		assertEquals("String", ty.polys().get(0).name());
		assertEquals("Number", ty.polys().get(1).name());
		assertEquals("l", ((TypedPattern)patt).var);
	}

//	(Cons { f: P, f: P }) - constructor with matching fields

	@Test
	public void testConstantConstructor() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("Nil"));
		assertNotNull(patt);
		assertTrue(patt instanceof ConstructorMatch);
		assertEquals("Nil", ((ConstructorMatch)patt).ctor);
		assertEquals(0, ((ConstructorMatch)patt).args.size());
	}

	@Test
	public void testConstructor() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("(Cons { tail: Nil })"));
		assertNotNull(patt);
		assertTrue(patt instanceof ConstructorMatch);
		assertEquals("Cons", ((ConstructorMatch)patt).ctor);
		assertEquals(1, ((ConstructorMatch)patt).args.size());
		ConstructorMatch.Field f = ((ConstructorMatch)patt).args.get(0);
		assertEquals("tail", f.field);
		assertTrue(f.patt instanceof ConstructorMatch);
		assertEquals("Nil", ((ConstructorMatch)f.patt).ctor);
		assertEquals(0, ((ConstructorMatch)f.patt).args.size());
	}


//	(P,P ...)    - tuple of patterns

	@Test
	public void testTuple2Vars() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("(l, m)"));
		assertNotNull(patt);
		assertTrue(patt instanceof TuplePattern);
		assertEquals(2, ((TuplePattern)patt).args.size());
		assertEquals("l", ((VarPattern)((TuplePattern)patt).args.get(0)).var);
		assertEquals("m", ((VarPattern)((TuplePattern)patt).args.get(1)).var);
	}

	@Test
	public void testTuple2() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("(List l, m)"));
		assertNotNull(patt);
		assertTrue(patt instanceof TuplePattern);
		assertEquals(2, ((TuplePattern)patt).args.size());
		TypedPattern foo = (TypedPattern)((TuplePattern)patt).args.get(0);
		assertEquals("List", foo.type.name());
		assertEquals("l", foo.var);
		assertEquals("m", ((VarPattern)((TuplePattern)patt).args.get(1)).var);
	}

//	Magic syntax for [], (P:var)

	@Test
	public void testMagicNil() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("[]"));
		assertNotNull(patt);
		assertTrue(patt instanceof ConstructorMatch);
		assertEquals("Nil", ((ConstructorMatch)patt).ctor);
		assertEquals(0, ((ConstructorMatch)patt).args.size());
	}

	@Test
	public void testMagicOneItemList() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("[a]"));
		assertNotNull(patt);
		assertTrue(patt instanceof ConstructorMatch);
		assertEquals("Cons", ((ConstructorMatch)patt).ctor);
		assertEquals(2, ((ConstructorMatch)patt).args.size());
		ConstructorMatch.Field f0 = ((ConstructorMatch)patt).args.get(0);
		assertEquals("head", f0.field);
		assertTrue(f0.patt instanceof VarPattern);
		assertEquals("a", ((VarPattern)f0.patt).var);
		ConstructorMatch.Field f1 = ((ConstructorMatch)patt).args.get(1);
		assertEquals("tail", f1.field);
		assertTrue(f1.patt instanceof ConstructorMatch);
		assertEquals("Nil", ((ConstructorMatch)f1.patt).ctor);
		assertEquals(0, ((ConstructorMatch)f1.patt).args.size());
	}

	@Test
	public void testMagicTwoItemList() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("[a,b]"));
		assertNotNull(patt);
		
		ConstructorMatch p2;
		{
			assertTrue(patt instanceof ConstructorMatch);
			assertEquals("Cons", ((ConstructorMatch)patt).ctor);
			assertEquals(2, ((ConstructorMatch)patt).args.size());
			ConstructorMatch.Field f0 = ((ConstructorMatch)patt).args.get(0);
			assertEquals("head", f0.field);
			assertTrue(f0.patt instanceof VarPattern);
			assertEquals("a", ((VarPattern)f0.patt).var);
			ConstructorMatch.Field f1 = ((ConstructorMatch)patt).args.get(1);
			assertEquals("tail", f1.field);
			assertTrue(f1.patt instanceof ConstructorMatch);
			p2 = (ConstructorMatch) f1.patt;
		}
		
		ConstructorMatch p3;
		{
			assertEquals("Cons", p2.ctor);
			assertEquals(2, p2.args.size());
			ConstructorMatch.Field f0 = p2.args.get(0);
			assertEquals("head", f0.field);
			assertTrue(f0.patt instanceof VarPattern);
			assertEquals("b", ((VarPattern)f0.patt).var);
			ConstructorMatch.Field f1 = p2.args.get(1);
			assertEquals("tail", f1.field);
			assertTrue(f1.patt instanceof ConstructorMatch);
			p3 = (ConstructorMatch) f1.patt;
		}
		
		assertEquals("Nil", p3.ctor);
		assertEquals(0, p3.args.size());
	}

	@Test
	public void testMagicHeadTailList() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("(a:l)"));
		assertNotNull(patt);
		assertTrue(patt instanceof ConstructorMatch);
		assertEquals("Cons", ((ConstructorMatch)patt).ctor);
		assertEquals(2, ((ConstructorMatch)patt).args.size());
		ConstructorMatch.Field f0 = ((ConstructorMatch)patt).args.get(0);
		assertEquals("head", f0.field);
		assertTrue(f0.patt instanceof VarPattern);
		assertEquals("a", ((VarPattern)f0.patt).var);
		ConstructorMatch.Field f1 = ((ConstructorMatch)patt).args.get(1);
		assertEquals("tail", f1.field);
		assertTrue(f1.patt instanceof VarPattern);
		assertEquals("a", ((VarPattern)f0.patt).var);
	}

//	Constants: numbers, strings, chars (others?)

	@Test
	public void testIntegerConstant() {
		PatternParser pp = new PatternParser();
		Object patt = pp.tryParsing(new Tokenizable("0"));
		assertNotNull(patt);
		assertTrue(patt instanceof ConstPattern);
		assertEquals(ConstPattern.INTEGER, ((ConstPattern)patt).type);
		assertEquals("0", ((ConstPattern)patt).value);
	}
}
