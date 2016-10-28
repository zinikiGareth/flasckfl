package org.flasck.flas.jsgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.typechecker.Type;
import org.junit.Test;

public class TestGeneratingStructs {
	static InputPosition posn = new InputPosition("test", 1, 1, null);

	@Test
	public void testACaseWithNoFields() throws IOException {
		RWStructDefn sd = new RWStructDefn(posn, "ME.Hello", true);
		JSTarget target = new JSTarget("ME");
		Generator gen = new Generator(null, target);
		gen.generate(sd);
		target.writeTo(new PrintWriter(System.out, true));
		assertEquals(3, target.forms.size());
		JSForm f = target.forms.get(1);
		assertNotNull(f);
		assertEquals("ME._Hello = function(v0) {\n  \"use strict\";\n  this._ctor = 'ME.Hello';\n}\n", f.toString());
		JSForm g = target.forms.get(2);
		assertNotNull(g);
		assertEquals("ME.Hello = function() {\n  \"use strict\";\n  return new ME._Hello({});\n}\n", g.toString());
	}


	@Test
	public void testACaseWithTwoFields() throws IOException {
		ImportPackage biScope = Builtin.builtins();
		Type str = (Type) biScope.get("String");
		Type nbr = (Type) biScope.get("Number");
		RWStructDefn sd = new RWStructDefn(posn, "ME.Hello", true);
		sd.addField(new RWStructField(posn, false, str, "name"));
		sd.addField(new RWStructField(posn, false, nbr, "quant"));
		JSTarget target = new JSTarget("ME");
		Generator gen = new Generator(null, target);
		gen.generate(sd);
		target.writeTo(new PrintWriter(System.out, true));
		assertEquals(3, target.forms.size());
		JSForm f = target.forms.get(1);
		assertNotNull(f);
		assertEquals("ME._Hello = function(v0) {\n  \"use strict\";\n  this._ctor = 'ME.Hello';\n  if (v0) {\n    if (v0.name) {\n      this.name = v0.name;\n    }\n    if (v0.quant) {\n      this.quant = v0.quant;\n    }\n  }\n  else {\n  }\n}\n", f.toString());
		JSForm g = target.forms.get(2);
		assertNotNull(g);
		assertEquals("ME.Hello = function(v0, v1) {\n  \"use strict\";\n  return new ME._Hello({name: v0, quant: v1});\n}\n", g.toString());
	}

}
