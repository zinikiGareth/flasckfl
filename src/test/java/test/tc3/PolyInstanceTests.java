package test.tc3;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.tc3.PolyInstance;
import org.junit.Test;

public class PolyInstanceTests {
	private static InputPosition pos = new InputPosition("BuiltIn", 1, 0, null, "<<builtin>>");
	private static PackageName poly = new PackageName(true);

	@Test
	public void aPolyInstanceHasASignature() {
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.string));
		assertEquals("Cons[String]", pi.signature());
	}

	@Test
	public void aPolyInstanceCanIncorporateItself() {
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.string));
		assertTrue(pi.incorporates(pos, pi));
	}

	@Test
	public void aPolyInstanceCanIncorporateSomethingElse() {
		PolyInstance list = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.string));
		PolyInstance cons = new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.string));
		assertTrue(list.incorporates(pos, cons));
	}

	@Test
	public void aPolyInstanceCannotIncorporateSomethingElseIfItIsNotAPolyInstance() {
		PolyInstance list = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.string));
		assertFalse(list.incorporates(pos, LoadBuiltins.number));
	}

	// I think this is overly dramatic, but all the variance/contravariance things confuse me
	@Test
	public void aPolyInstanceCannotIncorporateSomethingElseIfTheParametersAreNotTheSame() {
		PolyInstance listS = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.string));
		PolyInstance listN = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.number));
		assertFalse(listS.incorporates(pos, listN));
		assertFalse(listN.incorporates(pos, listS));
	}

	@Test
	public void aListOfAnyCanIncorporateAListOfSomethingElse() {
		PolyInstance listS = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.any));
		PolyInstance listN = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.number));
		assertTrue(listS.incorporates(pos, listN));
		assertFalse(listN.incorporates(pos, listS));
	}

	@Test
	public void aListOfAnyCanIncorporateAConsOfSomethingElse() {
		PolyInstance listS = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.any));
		PolyInstance listN = new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(new PolyType(pos, new SolidName(poly, "A"))));
		assertTrue(listS.incorporates(pos, listN));
		assertFalse(listN.incorporates(pos, listS));
	}
}
