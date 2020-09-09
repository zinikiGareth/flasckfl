package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;

public class FlimInstanceReader extends FlimTypeReader implements PendingType {
	private final FlimTypeReader parent;
	private final List<PendingType> args = new ArrayList<>();
	private InputPosition location;
	
	public FlimInstanceReader(ErrorReporter errors, FlimTypeReader parent) {
		super(errors);
		this.parent = parent;
	}

	@Override
	public void collect(PendingType ty) {
		args.add(ty);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		this.location = location;
		parent.collect(this);
	}

	@Override
	public Type resolve(ErrorReporter errors, Repository repository, List<PolyType> polys) {
		NamedType nt = (NamedType) args.get(0).resolve(errors, repository, polys);
		List<Type> inpolys = new ArrayList<>();
		for (int i=1;i<args.size();i++)
			inpolys.add(args.get(i).resolve(errors, repository, polys));
		return new PolyInstance(location, nt, inpolys);
	}

	@Override
	public TypeReference resolveAsRef(ErrorReporter errors, Repository repository, List<PolyType> polys) {
		PolyInstance pi = (PolyInstance) resolve(errors, repository, polys);
		List<TypeReference> inpolys = new ArrayList<>();
		for (int i=1;i<args.size();i++)
			inpolys.add(args.get(i).resolveAsRef(errors, repository, polys));
		TypeReference tr = new TypeReference(pi.location(), pi.name().baseName(), inpolys);
		tr.bind(pi);
		return tr;
	}
}
