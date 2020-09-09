package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

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
	public Type resolve(ErrorReporter errors, Repository repository) {
		NamedType nt = (NamedType) args.get(0).resolve(errors, repository);
		List<Type> polys = new ArrayList<>();
		for (int i=1;i<args.size();i++)
			polys.add(args.get(i).resolve(errors, repository));
		return new PolyInstance(location, nt, polys);
	}

	@Override
	public TypeReference resolveAsRef(ErrorReporter errors, Repository repository) {
		PolyInstance pi = (PolyInstance) resolve(errors, repository);
		List<TypeReference> polys = new ArrayList<>();
		for (int i=1;i<args.size();i++)
			polys.add(args.get(i).resolveAsRef(errors, repository));
		TypeReference tr = new TypeReference(pi.location(), pi.name().baseName(), polys);
		tr.bind(pi);
		return tr;
	}
}
