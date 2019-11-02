package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.zinutils.exceptions.NotImplementedException;

public class TypeConstraintSet implements UnifiableType {
	public class UnifiableApplication {
		private final List<Type> args;
		private final UnifiableType ret;

		public UnifiableApplication(List<Type> args, UnifiableType ret) {
			this.args = args;
			this.ret = ret;
		}
	}

	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final InputPosition pos;
	private final String id;
	private final Set<Type> incorporatedBys = new HashSet<>();
	private final Map<StructDefn, StructTypeConstraints> ctors = new TreeMap<>(StructDefn.nameComparator);
	private final Set<Type> types = new HashSet<>();
	private final Set<UnifiableApplication> applications = new HashSet<>();
	private Type resolvedTo;
	private int usedOrReturned = 0;
	private final Set<ConsolidateTypes> consolidations = new HashSet<>();
	
	public TypeConstraintSet(RepositoryReader r, CurrentTCState state, InputPosition pos, String id) {
		repository = r;
		this.state = state;
		this.pos = pos;
		this.id = id;
	}

	public boolean isResolved() {
		return resolvedTo != null;
	}
	
	@Override
	public Type resolve() {
		return resolve(true);
	}
	
	@Override
	public Type resolve(boolean hard) {
		if (resolvedTo != null)
			return resolvedTo;
		Set<Type> tys = new HashSet<Type>();
		for (Type t : types) {
			if (t instanceof StructDefn && ((StructDefn)t).hasPolys()) {
				StructDefn sd = (StructDefn) t;
				List<Type> polys = new ArrayList<>();
				// TODO: I think for type cases we should in fact insist on them specifying the polymorphic vars
				// We would then have them here (probably already as a PolyInstance!) ...
				for (PolyType p : sd.polys()) {
					polys.add(LoadBuiltins.any);
				}
				tys.add(new PolyInstance(sd, polys));
			} else
				tys.add(t);
		}

		// We have been explicitly told that these are true, usually through pattern matching
		// This is too broad; but I think we are going to need to do something like this ultimately, so just suck it up ...
		for (Entry<StructDefn, StructTypeConstraints> e : ctors.entrySet()) {
			StructDefn ty = e.getKey();
			if (!ty.hasPolys())
				tys.add(ty);
			else {
				StructTypeConstraints stc = ctors.get(ty);
				Map<PolyType, Type> polyMap = new HashMap<>();
				for (StructField f : stc.fields()) {
					PolyType pt = ty.findPoly(f.type);
					if (pt == null)
						continue;
					polyMap.put(pt, stc.get(f).resolve());
				}
				List<Type> polys = new ArrayList<>();
				for (PolyType p : ty.polys()) {
					if (polyMap.containsKey(p))
						polys.add(polyMap.get(p));
					else
						polys.add(LoadBuiltins.any);
				}
				tys.add(new PolyInstance(ty, polys));
			}
		}
		
		for (UnifiableApplication x : applications) {
			// I feel like this *could* get us into an infinite loop, but I don't think it actually can on account of how we introduce the return variable
			// and while it could possibly recurse, I don't think that can then refer back to us
			// If we do run into that problem, we should probably throw a special "UnresolvedReferenceException" here and then catch that in the loop
			// and then go around again
			// But at least make sure you have a test case before doing that ...
			List<Type> forApply = new ArrayList<>();
			for (Type t : x.args) {
				if (t instanceof UnifiableType)
					forApply.add(((UnifiableType)t).resolve());
				else
					forApply.add(t);
			}
			forApply.add(x.ret.resolve());
			tys.add(new Apply(forApply));
		}
		
		tys.addAll(incorporatedBys);
		
		List<UnifiableType> sameAs = new ArrayList<>();
		for (ConsolidateTypes ct : consolidations) {
			if (!ct.types.isEmpty())
				tys.add(ct.consolidatedAs());
			for (UnifiableType ut : ct.uts) {
				if (ut == this)
					continue;
				if (ut.isResolved())
					tys.add(ut.resolve());
				else
					sameAs.add(ut);
			}
		}
		
		HashSet<Type> all = new HashSet<Type>();
		for (Type ty : tys) {
			if (ty instanceof UnifiableType) {
				UnifiableType ut = (UnifiableType) ty;
				if (ut.isResolved())
					all.add(ut.resolve());
				else
					sameAs.add(ut);
			} else
				all.add(ty);
		}

		if (all.isEmpty()) {
			if (!hard) { // don't resolve just yet ...
				return null;
			}
			if (usedOrReturned > 0 || !sameAs.isEmpty())
				resolvedTo = state.nextPoly(pos);
			else
				resolvedTo = LoadBuiltins.any;
		} else if (all.size() == 1)
			resolvedTo = all.iterator().next();
		else {
			resolvedTo = repository.findUnionWith(all);
			if (resolvedTo == null) {
				// this is a legit type error
				throw new NotImplementedException("This should be a legit cannot merge type error: cannot unify " + all);
			}
		}

		for (UnifiableType ut : sameAs) {
			if (!ut.isResolved())
				ut.incorporatedBy(null, resolvedTo);
		}
		
		return resolvedTo;
	}
	
	@Override
	public void isReturned() {
		usedOrReturned++;
	}

	@Override
	public void isUsed() {
		usedOrReturned++;
	}

	@Override
	public void incorporatedBy(InputPosition pos, Type incorporator) {
		incorporatedBys.add(incorporator);
	}

	@Override
	public String signature() {
		if (resolvedTo == null)
			throw new NotImplementedException("Has not been resolved");
		return resolvedTo.signature();
	}

	@Override
	public int argCount() {
		if (resolvedTo == null)
			throw new NotImplementedException("Has not been resolved");
		return resolvedTo.argCount();
	}

	@Override
	public Type get(int pos) {
		if (resolvedTo == null)
			throw new NotImplementedException("Has not been resolved");
		return resolvedTo.get(pos);
	}

	@Override
	public boolean incorporates(Type other) {
		// Is this true?
		// I think that the case that has arisen is if you have a UT for a function "f", then it could be asked if it incorporates something ...
		// So you have to assume that f : ?->? and we are being told something about this type with respect to "other" ...
		throw new NotImplementedException("The type algorithm should recognize us and call incorporatedBy instead");
	}

	@Override
	public StructTypeConstraints canBeStruct(StructDefn sd) {
		if (!ctors.containsKey(sd))
			ctors.put(sd, new StructFieldConstraints(repository, sd));
		return ctors.get(sd);
	}

	@Override
	public void canBeType(Type ofType) {
		types.add(ofType);
	}
	
	@Override
	public UnifiableType canBeAppliedTo(List<Type> args) {
		// Here we introduce a new variable that we will be able to constrain
		UnifiableType ret = state.createUT();
		for (Type ty : args) {
			if (ty instanceof UnifiableType)
				((UnifiableType)ty).isUsed();
		}
		applications.add(new UnifiableApplication(args, ret));
		return ret;
	}

	@Override
	public void consolidatesWith(ConsolidateTypes consolidateTypes) {
		consolidations.add(consolidateTypes);
	}
	
	@Override
	public void isPassed(InputPosition loc, Type ai) {
		// This is the same implementation as "canBeType" - is that correct?
		types.add(ai);
	}

	@Override
	public String toString() {
		if (isResolved())
			return signature();
		else
			return "TCS{" + id + "}";
	}
}
