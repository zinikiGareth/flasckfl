package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.TypeExprParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class TypeExprParsingTests {
	TypeExprParser parser = new TypeExprParser();

	@Test
	public void aSimpleTypeName() {
		Object tt = parser.tryParsing(new Tokenizable("String"));
		assertTrue(tt instanceof TypeReference);
		assertEquals("String", ((TypeReference)tt).name());
	}

	@Test
	public void aSimpleFunctionType() {
		Object tt = parser.tryParsing(new Tokenizable("String->Number"));
		assertTrue(tt instanceof FunctionTypeReference);
		FunctionTypeReference ftr = (FunctionTypeReference)tt;
		assertEquals(2, ftr.args.size());
		assertEquals("String", ftr.args.get(0).name());
		assertEquals("Number", ftr.args.get(1).name());
	}

	@Test
	public void aSimpleTupleType() {
		parser.tryParsing(new Tokenizable("(String,Number)"));
	}

	@Test
	public void aPolymorphicTypeWithSimpleArgument() {
		Object tt = parser.tryParsing(new Tokenizable("List[String]"));
		assertTrue(tt instanceof TypeReference);
		TypeReference tr = (TypeReference) tt;
		assertEquals("List", tr.name());
		assertTrue(tr.hasPolys());
		assertEquals(1, tr.polys().size());
		assertEquals("String", tr.polys().get(0).name());
	}

	@Test
	public void aPolymorphicTypeWithFunctionArgument() {
		Object tt = parser.tryParsing(new Tokenizable("List[String->Number]"));
		assertTrue(tt instanceof TypeReference);
		TypeReference tr = (TypeReference) tt;
		assertEquals("List", tr.name());
		assertTrue(tr.hasPolys());
		assertEquals(1, tr.polys().size());
		TypeReference p0 = tr.polys().get(0);
		assertTrue(p0 instanceof FunctionTypeReference);
		FunctionTypeReference ftr = (FunctionTypeReference)p0;
		assertEquals(2, ftr.args.size());
		assertEquals("String", ftr.args.get(0).name());
		assertEquals("Number", ftr.args.get(1).name());
	}

	@Test
	public void aPolymorphicTypeWithTupleArgument() {
		parser.tryParsing(new Tokenizable("List[(String, Number)]"));
	}
}
