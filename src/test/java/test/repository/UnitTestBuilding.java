package test.repository;

import static org.junit.Assert.*;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.junit.Test;

import test.parsing.ExprMatcher;

public class UnitTestBuilding {
	private InputPosition pos = new InputPosition("fred", 10, 0, "hello");

	@Test
	public void addingAFieldToADataDecl() {
		UnitDataDeclaration decl = new UnitDataDeclaration(null, null, null);
		decl.field(new UnresolvedVar(pos, "x"), new StringLiteral(pos, "hello"));
		assertEquals(1, decl.fields.size());
		Assignment f = decl.fields.get(0);
		assertEquals("x", f.field.var);
		assertThat(f.value, ExprMatcher.string("hello"));
	}

}
