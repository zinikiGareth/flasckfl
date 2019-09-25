package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ConstraintUnification {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private CurrentTCState state = new FunctionGroupTCState();
	
	@Test
	public void ifWeDontDoAnythingWeEndUpAskingForAPolyVar() {
		UnifiableType ut = state.functionParameter(pos, "v");
		Type ty = ut.resolve();
		assertTrue(ty instanceof PolyType);
		assertEquals("A", ((PolyType)ty).name());
		assertEquals(pos, ((PolyType)ty).location());
	}

	@Test
	public void twoTypesGetTwoPolyVars() {
		UnifiableType uv = state.functionParameter(pos, "v");
		UnifiableType uw = state.functionParameter(pos, "w");
		Type tv = uv.resolve();
		assertTrue(tv instanceof PolyType);
		Type tw = uw.resolve();
		assertTrue(tw instanceof PolyType);
		assertEquals("A", ((PolyType)tv).name());
		assertEquals("B", ((PolyType)tw).name());
	}
	
	@Test
	public void oneIncoportatedByConstraintCreatesAnIdentity() {
		UnifiableType ut = state.functionParameter(pos, "v");
		ut.incorporatedBy(pos, LoadBuiltins.number);
		Type ty = ut.resolve();
		assertEquals(LoadBuiltins.number, ty);
	}
}
