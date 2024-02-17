package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.TupleTypeReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.TDAProvideType;
import org.flasck.flas.parser.TypeExprParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TypeExprParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	ErrorReporter errors = context.mock(ErrorReporter.class);
	TypeExprParser parser = new TypeExprParser(errors);
	List<TypeReference> pt = new ArrayList<>();

	class Listener implements TDAProvideType {
		@Override
		public void provide(TypeReference ty) {
			pt.add(ty);
		}
	}

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(Locatable.class)), with(any(Locatable.class)));
		}});
	}

	@Test
	public void aSimpleTypeName() {
		parser.tryParsing(new Tokenizable("String"), new Listener());
		assertEquals(1, pt.size());
		assertEquals("String", pt.get(0).name());
	}

	@Test
	public void aSimpleFunctionType() {
		parseTypeExpr("String->Number");
		assertEquals(1, pt.size());
		TypeReference tt = pt.get(0);
		assertTrue(tt instanceof FunctionTypeReference);
		FunctionTypeReference ftr = (FunctionTypeReference)tt;
		assertEquals(2, ftr.args.size());
		assertEquals("String", ftr.args.get(0).name());
		assertEquals("Number", ftr.args.get(1).name());
	}

	@Test
	public void aTwoArgFunctionType() {
		parseTypeExpr("String->Number->String");
		assertEquals(1, pt.size());
		TypeReference tt = pt.get(0);
		assertTrue(tt instanceof FunctionTypeReference);
		FunctionTypeReference ftr = (FunctionTypeReference)tt;
		assertEquals(3, ftr.args.size());
		assertEquals("String", ftr.args.get(0).name());
		assertEquals("Number", ftr.args.get(1).name());
		assertEquals("String", ftr.args.get(2).name());
	}

	@Test
	public void aNestedFunctionType() {
		parseTypeExpr("String->(Number->Boolean)->String");
		assertEquals(1, pt.size());
		TypeReference tt = pt.get(0);
		assertTrue(tt instanceof FunctionTypeReference);
		FunctionTypeReference ftr = (FunctionTypeReference)tt;
		assertEquals(3, ftr.args.size());
		assertEquals("String", ftr.args.get(0).name());
		TypeReference t1 = ftr.args.get(1);
		assertTrue(t1 instanceof FunctionTypeReference);
		FunctionTypeReference ft1 = (FunctionTypeReference) t1;
		assertEquals("Number", ft1.args.get(0).name());
		assertEquals("Boolean", ft1.args.get(1).name());
		assertEquals("String", ftr.args.get(2).name());
	}

	@Test
	public void aSimpleTupleType() {
		parseTypeExpr("(String,Number)");
		assertEquals(1, pt.size());
		TypeReference tt = pt.get(0);
		assertTrue(tt instanceof TupleTypeReference);
		TupleTypeReference ttr = (TupleTypeReference) tt;
		assertEquals(2, ttr.members.size());
		assertEquals("String", ttr.members.get(0).name());
		assertEquals("Number", ttr.members.get(1).name());
	}

	@Test
	public void aFunctionTupleType() {
		parseTypeExpr("(String,Number->Boolean)");
		assertEquals(1, pt.size());
		TypeReference tt = pt.get(0);
		assertTrue(tt instanceof TupleTypeReference);
		TupleTypeReference ttr = (TupleTypeReference) tt;
		assertEquals(2, ttr.members.size());
		assertEquals("String", ttr.members.get(0).name());
		assertTrue(ttr.members.get(1) instanceof FunctionTypeReference);
		FunctionTypeReference ftr = (FunctionTypeReference) ttr.members.get(1);
		assertEquals("Number", ftr.args.get(0).name());
		assertEquals("Boolean", ftr.args.get(1).name());
	}

	@Test
	public void aFunctionReturningATupleType() {
		parseTypeExpr("Number->(String,Boolean)");
		assertEquals(1, pt.size());
		TypeReference tt = pt.get(0);
		assertTrue(tt instanceof FunctionTypeReference);
		FunctionTypeReference ttr = (FunctionTypeReference) tt;
		assertEquals(2, ttr.args.size());
		assertEquals("Number", ttr.args.get(0).name());
		assertTrue(ttr.args.get(1) instanceof TupleTypeReference);
		TupleTypeReference ftr = (TupleTypeReference) ttr.args.get(1);
		assertEquals("String", ftr.members.get(0).name());
		assertEquals("Boolean", ftr.members.get(1).name());
	}
	
	@Test
	public void aPolymorphicTypeWithSimpleArgument() {
		parseTypeExpr("List[String]");
		assertEquals(1, pt.size());
		TypeReference tr = pt.get(0);
		assertEquals("List", tr.name());
		assertTrue(tr.hasPolys());
		assertEquals(1, tr.polys().size());
		assertEquals("String", tr.polys().get(0).name());
	}

	@Test
	public void aPolymorphicTypeWithFunctionArgument() {
		parseTypeExpr("List[String->Number]");
		assertEquals(1, pt.size());
		TypeReference tr = pt.get(0);
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
		parseTypeExpr("List[(String, Number)]");
		assertEquals(1, pt.size());
		TypeReference tr = pt.get(0);
		assertEquals("List", tr.name());
		assertTrue(tr.hasPolys());
		assertEquals(1, tr.polys().size());
		TypeReference p0 = tr.polys().get(0);
		assertTrue(p0 instanceof TupleTypeReference);
		TupleTypeReference ftr = (TupleTypeReference)p0;
		assertEquals(2, ftr.members.size());
		assertEquals("String", ftr.members.get(0).name());
		assertEquals("Number", ftr.members.get(1).name());
	}

	private void parseTypeExpr(String text) {
		Tokenizable toks = new Tokenizable(text);
		parser.tryParsing(toks, new Listener());
		assertFalse(toks.hasMoreContent(errors));
	}
}
