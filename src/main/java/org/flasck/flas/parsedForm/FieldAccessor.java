package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.tc3.Type;

public interface FieldAccessor {
	Type type();
	Expr acor(Expr from);
	int acorArgCount();
}
