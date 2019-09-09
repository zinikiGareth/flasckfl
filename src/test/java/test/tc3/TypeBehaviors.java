package test.tc3;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Primitive;
import org.junit.Test;

public class TypeBehaviors {

	@Test(expected=RuntimeException.class)
	public void applyRequiresArgs() {
		new Apply();
	}

	@Test(expected=RuntimeException.class)
	public void applyRequiresAtLeastTwoArgsNotOne() {
		Primitive number = new Primitive("Number");
		new Apply(number);
	}

	@Test
	public void applyCanHandleASimpleMapping() {
		Primitive number = new Primitive("Number");
		Apply a = new Apply(number, number);
		assertEquals(1, a.argCount());
		assertEquals(number, a.get(0));
		assertEquals(number, a.get(1));
		assertEquals("Number->Number", a.signature());
	}

}
