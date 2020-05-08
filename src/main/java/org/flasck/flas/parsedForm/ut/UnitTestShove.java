package org.flasck.flas.parsedForm.ut;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestShove implements UnitTestStep {
	public final List<UnresolvedVar> slots;
	public final Expr value;

	public UnitTestShove(List<UnresolvedVar> slots, Expr value) {
		this.slots = slots;
		this.value = value;
	}
}
