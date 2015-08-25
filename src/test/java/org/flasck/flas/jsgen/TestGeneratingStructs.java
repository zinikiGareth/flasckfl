package org.flasck.flas.jsgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.typechecker.Type;
import org.junit.Test;

public class TestGeneratingStructs {

	@Test
	public void testACaseWithNoFields() throws IOException {
		StructDefn sd = new StructDefn(null, "ME.Hello", true);
		ErrorResult errors = new ErrorResult();
		JSTarget target = new JSTarget("ME");
		Generator gen = new Generator(errors, target);
		gen.generate(sd);
		target.writeTo(new PrintWriter(System.out, true));
		assertEquals(4, target.forms.size());
		JSForm f = target.forms.get(1);
		assertNotNull(f);
		assertEquals("ME._Hello = function(v0) {\n  \"use strict\";\n  this._ctor = 'ME.Hello';\n}\n", f.toString());
		JSForm g = target.forms.get(2);
		assertNotNull(g);
		assertEquals("ME.Hello = function() {\n  \"use strict\";\n  return new ME._Hello({});\n}\n", g.toString());
	}


	@Test
	public void testACaseWithTwoFields() throws IOException {
		StructDefn sd = new StructDefn(null, "ME.Hello", true);
		sd.addField(new StructField(Type.reference(null, "String"), "name"));
		sd.addField(new StructField(Type.reference(null, "Number"), "quant"));
		ErrorResult errors = new ErrorResult();
		JSTarget target = new JSTarget("ME");
		Generator gen = new Generator(errors, target);
		gen.generate(sd);
		target.writeTo(new PrintWriter(System.out, true));
		assertEquals(4, target.forms.size());
		JSForm f = target.forms.get(1);
		assertNotNull(f);
		assertEquals("ME._Hello = function(v0) {\n  \"use strict\";\n  this._ctor = 'ME.Hello';\n  if (v0) {\n    if (v0.name) {\n      this.name = v0.name;\n    }\n    if (v0.quant) {\n      this.quant = v0.quant;\n    }\n  }\n  else {\n  }\n}\n", f.toString());
		JSForm g = target.forms.get(2);
		assertNotNull(g);
		assertEquals("ME.Hello = function(v0, v1) {\n  \"use strict\";\n  return new ME._Hello({name: v0, quant: v1});\n}\n", g.toString());
	}

}
