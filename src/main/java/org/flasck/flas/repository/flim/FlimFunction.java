package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.zinutils.exceptions.CantHappenException;

public class FlimFunction extends FlimTypeReader implements TDAParsing {
	private final Repository repository;
	private final FunctionName fn;
	private final List<PolyType> polys = new ArrayList<>();
	private final List<PendingType> args = new ArrayList<>();
	private StateHolder holder;
	private FunctionDefinition fd;

	public FlimFunction(ErrorReporter errors, Repository repository, FunctionName fn) {
		super(errors);
		this.repository = repository;
		this.fn = fn;
	}

	public void create() {
		fd = new FunctionDefinition(fn, args.size(), holder);
		fd.dontGenerate();
		repository.functionDefn(errors, fd);
	}

	@Override
	protected void definePolyVar(InputPosition location, String text) {
		polys.add(new PolyType(location, new SolidName(fn, text)));
	}

	public void collect(PendingType ty) {
		args.add(ty);
	}
	
	public void bindType() {
		if (args.size() != 1)
			throw new CantHappenException("should have one arg at the end of the day, even if it's an apply");
		fd.bindType(args.get(0).resolve(errors, repository, polys));
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create();
	}
}
