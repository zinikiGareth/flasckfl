package test.tc3;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.tc3.PolyInstance;
import org.junit.Test;

public class PolyInstanceTests {

	@Test
	public void aPolyInstanceHasASignature() {
		PolyInstance pi = new PolyInstance(LoadBuiltins.cons, Arrays.asList(LoadBuiltins.string));
		assertEquals("Cons[String]", pi.signature());
	}

}
