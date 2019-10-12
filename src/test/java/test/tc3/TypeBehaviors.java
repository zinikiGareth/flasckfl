package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
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
		assertTrue(number.incorporates(number));
	}

	@Test
	public void numberDoesNotIncorporateString() {
		Primitive number = new Primitive(pos, "Number");
		Primitive string = new Primitive(pos, "String");
		assertFalse(number.incorporates(string));
	}

}
