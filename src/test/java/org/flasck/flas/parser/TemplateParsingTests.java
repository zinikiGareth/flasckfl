package org.flasck.flas.parser;

import static org.junit.Assert.*;

import org.flasck.flas.commonBase.template.TemplateIntro;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class TemplateParsingTests {
	private IntroParser parser = new IntroParser(null);
	
	@Test
	public void testATemplateCanHaveNoNameForTheMainTemplate() {
		Object ret = parser.tryParsing(new Tokenizable("template"));
		assertNotNull(ret);
		assertTrue(ret instanceof TemplateIntro);
		assertNull(((TemplateIntro)ret).name);
	}

	@Test
	public void testATemplateCanANameForANestedTemplate() {
		Object ret = parser.tryParsing(new Tokenizable("template inner"));
		assertNotNull(ret);
		assertTrue(ret instanceof TemplateIntro);
		assertEquals("inner", ((TemplateIntro)ret).name);
	}

	@Test
	public void testATemplateCanHaveVarTokensAfterAName() {
		Object ret = parser.tryParsing(new Tokenizable("template inner foo bar"));
		assertNotNull(ret);
		assertTrue(ret instanceof TemplateIntro);
		assertEquals(2, ((TemplateIntro)ret).args.size());
		assertEquals("foo", ((TemplateIntro)ret).args.get(0).text);
		assertEquals("bar", ((TemplateIntro)ret).args.get(1).text);
	}

	@Test
	public void testATemplateCannotHaveExprTokensAfterAName() {
		Object ret = parser.tryParsing(new Tokenizable("template inner (foo)"));
		assertNotNull(ret);
		assertTrue(ret instanceof ErrorResult);
		assertEquals(1, ((ErrorResult)ret).count());
	}

	@Test
	public void testATemplateCannotHaveATypeName() {
		Object ret = parser.tryParsing(new Tokenizable("template Class"));
		assertNotNull(ret);
		assertTrue(ret instanceof ErrorResult);
		assertEquals(1, ((ErrorResult)ret).count());
	}

}
