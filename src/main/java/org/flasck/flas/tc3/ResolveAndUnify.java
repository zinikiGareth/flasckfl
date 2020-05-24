package org.flasck.flas.tc3;

import java.util.HashSet;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnionTypeDefn.Unifier;
import org.flasck.flas.repository.RepositoryReader;

public class ResolveAndUnify implements Unifier {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final InputPosition pos = new InputPosition("unknown", 1, 0, "");

	public ResolveAndUnify(ErrorReporter errors, RepositoryReader repository) {
		this.errors = errors;
		this.repository = repository;
	}

	@Override
	public Type unify(Set<Type> tr) {
		// If we get here, we are already trying to resolve TCSs.
		// DO NOT introduce any more
		// This function needs to:
		//   - identify all the cast-iron types in here
		//     - include resolved UTs in that list in lieu of the generic UT *IF* they are resolved
		//   - if no UTs
		//     - try and find a suitable union
		//     - return NULL if it really can't be done - asked to unify Number and String, for example
		//   - if any unresolved UTs
		//     - merge them and add ALL constraints
		//     - and return the first one as the value
		
		// Note that the return type here may itself be polymorphic and contain UTs that need resolving
		
		// Separate and collect the resolvable and unresolvable ones
		Set<Type> rs = new HashSet<>();
		Set<UnifiableType> uts = new HashSet<>();
		for (Type t : tr) {
			if (t instanceof UnifiableType) {
				Type r = ((UnifiableType)t).resolve(errors, false);
				if (r == null)
					uts.add((UnifiableType) t);
				else
					rs.add(r);
			} else
				rs.add(t);
		}
		if (uts.isEmpty()) // everything was resolved
			return repository.findUnionWith(rs, this);
		
		for (UnifiableType ut : uts) {
			for (Type r : rs)
				ut.canBeType(pos, r);
			for (UnifiableType o : uts)
				if (o != ut) {
					ut.canBeType(pos, o);
					o.canBeType(pos, ut);
				}
		}
		return uts.iterator().next();
	}

}
