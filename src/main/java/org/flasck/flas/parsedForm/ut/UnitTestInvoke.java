package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.zinutils.exceptions.NotImplementedException;

public class UnitTestInvoke implements UnitTestStep {
	public final Expr expr;
	private Expr converted;
	
	public UnitTestInvoke(Expr expr) {
		this.expr = expr;
	}
	
	public void conversion(Expr converted) {
		this.converted = converted;
	}

	public boolean isConverted() {
		return converted != null;
	}

	public Expr converted() {
		if (converted != null)
			return converted;
		else
			throw new NotImplementedException("There is no converted function");
	}
}
