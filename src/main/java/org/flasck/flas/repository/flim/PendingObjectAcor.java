package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;

public class PendingObjectAcor extends FlimTypeReader {
	private final FunctionName fn;
	private PendingType type;

	public PendingObjectAcor(ErrorReporter errors, FunctionName fn) {
		super(errors);
		this.fn = fn;
	}

	@Override
	public void collect(PendingType ty) {
		this.type = ty;
	}
	
	public ObjectAccessor resolve(ErrorReporter errors, Repository repository, ObjectDefn od, List<PolyType> polys) {
		int nargs = 0;
		Type ty = type.resolve(errors, repository, polys);
		if (ty instanceof Apply)
			nargs = ((Apply)ty).argCount();
		FunctionDefinition fd = new FunctionDefinition(fn, nargs, od);
		fd.dontGenerate();
		fd.bindType(ty);
		ObjectAccessor oa = new ObjectAccessor(od, fd);
		oa.dontGenerate();
		repository.newObjectAccessor(errors, oa);
		return oa;
	}
}
