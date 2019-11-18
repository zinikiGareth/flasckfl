package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Primitive;
import org.junit.Test;

public class TypeBehaviors {
	private static InputPosition pos = new InputPosition("BuiltIn", 1, 0, "<<builtin>>");

	@Test(expected=RuntimeException.class)
	public void applyRequiresArgs() {
		new Apply();
	}

	@Test(expected=RuntimeException.class)
	public void applyRequiresAtLeastTwoArgsNotOne() {
		Primitive number = new Primitive(pos, "Number");
		new Apply(number);
	}

	@Test
	public void applyCanHandleASimpleMapping() {
		Primitive number = new Primitive(pos, "Number");
		Apply a = new Apply(number, number);
		assertEquals(1, a.argCount());
		assertEquals(number, a.get(0));
		assertEquals(number, a.get(1));
		assertEquals("Number->Number", a.signature());
	}
	
	@Test
	public void numberIncorporatesNumber() {
		Primitive number = new Primitive(pos, "Number");
		assertTrue(number.incorporates(pos, number));
	}

	@Test
	public void numberDoesNotIncorporateString() {
		Primitive number = new Primitive(pos, "Number");
		Primitive string = new Primitive(pos, "String");
		assertFalse(number.incorporates(pos, string));
	}

	@Test
	public void anyIncorporatesString() {
		assertTrue(LoadBuiltins.any.incorporates(pos, LoadBuiltins.string));
	}

	@Test
	public void contractIncorporatesAnArbitraryContract() {
		assertTrue(LoadBuiltins.contract.incorporates(pos, new ContractDecl(pos, pos, new SolidName(null, "Svc"))));
	}

	@Test
	public void contractDoesNotIncorporateString() {
		assertFalse(LoadBuiltins.contract.incorporates(pos, LoadBuiltins.string));
	}

	@Test
	public void applyInsertsParensAroundNestedApply() {
		Primitive number = new Primitive(pos, "Number");
		Apply f = new Apply(number, number);
		assertEquals("Number->Number", f.signature());
		Apply hof = new Apply(f, number);
		assertEquals("(Number->Number)->Number", hof.signature());
	}
	
	@Test
	public void applyDoesNotInsertParensAroundApplyAtEnd() {
		Primitive number = new Primitive(pos, "Number");
		Apply f = new Apply(number, number);
		assertEquals("Number->Number", f.signature());
		Apply hof = new Apply(number, f);
		assertEquals("Number->Number->Number", hof.signature());
	}
	
	@Test
	public void aStructCanIncorporateItself() {
		assertTrue(LoadBuiltins.cons.incorporates(pos, LoadBuiltins.cons));
	}

	@Test
	public void aStructCannotIncorporateNumber() {
		assertFalse(LoadBuiltins.cons.incorporates(pos, LoadBuiltins.number));
	}

	@Test
	public void aUnionCanIncorporateItself() {
		assertTrue(LoadBuiltins.list.incorporates(pos, LoadBuiltins.list));
	}

	@Test
	public void aUnionCanIncorporateAMemberStruct() {
		assertTrue(LoadBuiltins.list.incorporates(pos, LoadBuiltins.cons));
	}
}
