package org.flasck.flas.hsie;

import static org.junit.Assert.assertNotNull;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.junit.Ignore;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

// Although these are tests, they are really just to make sure that the data
// we enter in HSIETestData is valid from programs.
@Ignore
public class HSIECodeGenerator {

	@Test
	public void testConvertingIdOf1() {
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = plus1 1"));
		FunctionDefinition f = new FunctionDefinition(Type.FUNCTION, "f", 0, CollectionUtils.listOf(c1));
		HSIEForm form = HSIE.handle(f);
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.plus1Of1(), form);
	}

	// This is a pathological case of LET with vars
	@Test
	public void testConvertingIdDecode() {
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = id (decode (id 32))"));
		FunctionDefinition f = new FunctionDefinition(Type.FUNCTION, "f", 0, CollectionUtils.listOf(c1));
		HSIEForm form = HSIE.handle(f);
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.idDecode(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction1() {
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		FunctionDefinition f = new FunctionDefinition(Type.FUNCTION, "f", 1, CollectionUtils.listOf(c1));
		HSIEForm form = HSIE.handle(f);
		assertNotNull(form);
		form.dump();
		HSIETestData.assertHSIE(HSIETestData.rdf1(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction2() {
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		FunctionDefinition f = new FunctionDefinition(Type.FUNCTION, "g", 1, CollectionUtils.listOf(c1));
		HSIEForm form = HSIE.handle(f);
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.rdf2(), form);
	}
}
