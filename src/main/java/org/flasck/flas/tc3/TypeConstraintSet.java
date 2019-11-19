package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.zinutils.exceptions.NotImplementedException;

public class TypeConstraintSet implements UnifiableType {
	public class Comment {
		private final InputPosition pos;
		private final String msg;

		public Comment(InputPosition pos, String msg) {
			this.pos = pos;
			this.msg = msg;
		}
		
		@Override
		public String toString() {
			return pos + " - " + msg;
		}
	}

	public class UnifiableApplication {
		private final List<Type> args;
		private final Type ret;

		public UnifiableApplication(List<Type> args, Type ret) {
			this.args = args;
			this.ret = ret;
		}

		public Type asApply() {
			return new Apply(args, ret);
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
	private final List<Comment> comments = new ArrayList<>();
	
	public TypeConstraintSet(RepositoryReader r, CurrentTCState state, InputPosition pos, String id, String motive) {
		repository = r;
		this.state = state;
		this.pos = pos;
		this.id = id;
		comments.add(new Comment(pos, "created because " + motive));
//		try {
//			throw new RuntimeException(id + " " + motive);
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
	}

	public boolean isResolved() {
		return resolvedTo != null;
	}
	
	@Override
	public boolean enhance() {
		boolean again = false;
		while (true) {
			List<Type> refs = new ArrayList<>();
			for (Type t : types) {
				if (t instanceof TypeConstraintSet) {
					TypeConstraintSet other = (TypeConstraintSet) t;
					if (!other.types.contains(this)) {
						other.canBeType(pos, this);
						again = true;
					}
					for (Type t2 : other.types) {
						if (t2 != this && !types.contains(t2))
							refs.add(t2);
					}
				}
			}
			if (refs.isEmpty())
				return again;
			again = true;
//			System.out.println("Adding " + refs + " to " + asTCS());
			types.addAll(refs);
		}
	}
	
	@Override
	public Type resolve(ErrorReporter errors, boolean hard) {
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
					polyMap.put(pt, stc.get(f).resolve(errors, true));
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
		
		if (applications.size() == 1)
			tys.add(applications.iterator().next().asApply());
		else if (!applications.isEmpty()) {
			List<List<Type>> args = new ArrayList<>();
			List<Type> ret = new ArrayList<>();
			for (UnifiableApplication x : applications) {
				// I feel like this *could* get us into an infinite loop, but I don't think it actually can on account of how we introduce the return variable
				// and while it could possibly recurse, I don't think that can then refer back to us
				// If we do run into that problem, we should probably throw a special "UnresolvedReferenceException" here and then catch that in the loop
				// and then go around again
				// But at least make sure you have a test case before doing that ...
				int k = 0;
				for (Type t : x.args) {
					if (args.size() == k)
						args.add(new ArrayList<Type>());
					args.get(k++).add(t);
				}
				ret.add(x.ret);
			}
			List<Type> cargs = new ArrayList<>();
			for (List<Type> a : args)
				cargs.add(state.consolidate(pos, a));
			tys.add(new Apply(cargs, state.consolidate(pos, ret)));
		}
		
		tys.addAll(incorporatedBys);
		
		List<UnifiableType> sameAs = new ArrayList<>();
		HashSet<Type> all = new HashSet<Type>();
		for (Type ty : tys) {
			if (ty instanceof UnifiableType) {
				UnifiableType ut = (UnifiableType) ty;
				if (ut.isResolved())
					all.add(ut.resolve(errors, true));
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
				TreeSet<String> msg = new TreeSet<>();
				for (Type ty : all)
					msg.add(ty.signature());
//				System.out.println("Unify: " + debugInfo());
				errors.message(pos, "unable to unify " + String.join(", ", msg));
				return new ErrorType();
			}
		}

		for (UnifiableType ut : sameAs) {
			if (!ut.isResolved())
				ut.incorporatedBy(this.pos, resolvedTo);
		}
		
		return resolvedTo;
	}

	@Override
	public void isReturned(InputPosition pos) {
		usedOrReturned++;
	}

	@Override
	public void isUsed(InputPosition pos) {
		usedOrReturned++;
	}

	@Override
	public void incorporatedBy(InputPosition pos, Type incorporator) {
//		if (pos == null) {
//			try {
//				throw new RuntimeException(incorporator.toString());
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}
		incorporatedBys.add(incorporator);
		String t;
		if (incorporator instanceof TypeConstraintSet)
			t = ((TypeConstraintSet)incorporator).id;
		else if (incorporator instanceof PolyType)
			t = ((PolyType)incorporator).shortName();
		else if (incorporator instanceof NamedType)
			t = ((NamedType)incorporator).name().uniqueName();
		else
			t = incorporator.signature();
		comments.add(new Comment(pos, "incorporated by " + t));
	}

	@Override
	public String signature() {
		if (resolvedTo == null)
			return asTCS();
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
	public boolean incorporates(InputPosition pos, Type other) {
		this.isPassed(pos, other);
		return true;
	}

	@Override
	public StructTypeConstraints canBeStruct(InputPosition pos, StructDefn sd) {
		if (!ctors.containsKey(sd))
			ctors.put(sd, new StructFieldConstraints(repository, sd));
		return ctors.get(sd);
	}

	@Override
	public void canBeType(InputPosition pos, Type ofType) {
		if (ofType == null)
			throw new NotImplementedException("types cannot be null");
		types.add(ofType);
	}
	
	@Override
	public UnifiableType canBeAppliedTo(InputPosition pos, List<Type> args) {
		// Here we introduce a new variable that we will be able to constrain
		UnifiableType ret = state.createUT(pos, "unknown");
		for (Type ty : args) {
			if (ty instanceof UnifiableType)
				((UnifiableType)ty).isUsed(pos);
		}
		addApplication(args, ret);
		return ret;
	}

	void consolidatedApplication(Apply a) {
		List<Type> l = new ArrayList<>(a.tys);
		Type ret = l.remove(l.size()-1);
		addApplication(l, ret);
	}
	
	private void addApplication(List<Type> args, Type ret) {
		applications.add(new UnifiableApplication(args, ret));
	}

	@Override
	public void determinedType(Type ofType) {
		if (ofType == null)
			throw new NotImplementedException("types cannot be null");
		if (ofType instanceof Apply) {
			Apply a = (Apply) ofType;
			addApplication(a.tys.subList(0, a.tys.size()-1), a.tys.get(a.tys.size()-1));
		} else
			types.add(ofType);
	}

	@Override
	public void isPassed(InputPosition loc, Type ai) {
		// This is the same implementation as "canBeType" - is that correct?
		types.add(ai);
	}

	public String asTCS() {
		return "TCS{" + (pos == null? "NULL":pos.inFile()) + ":" + id + "}";
	}

	public String debugInfo() {
		StringBuilder ret = new StringBuilder();
		ret.append(asTCS());
		ret.append(" => ");
		if (this.isResolved()) {
			ret.append("{");
			ret.append(resolvedTo);
			ret.append("}");
		}
		ret.append(types);
		ret.append(" // ");
		ret.append(comments);
		return ret.toString();
	}
	
	@Override
	public String toString() {
		if (isResolved())
			return signature();
		else
			return asTCS();
	}
}
