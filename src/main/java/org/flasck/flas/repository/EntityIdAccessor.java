package org.flasck.flas.repository;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class EntityIdAccessor implements FieldAccessor {

	@Override
	public Type type() {
		// but I think it's LoadBuiltins.string
		throw new NotImplementedException();
	}

	@Override
	public Expr acor(Expr from) {
		return new MakeAcor(from.location(), FunctionName.function(from.location(), new SolidName(new PackageName(true), "Entity"), "_field_id"), from, 0);
	}

	@Override
	public int acorArgCount() {
		// but I think it's 0
		throw new NotImplementedException();
	}

}
