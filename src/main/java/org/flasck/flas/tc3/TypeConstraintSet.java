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
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.zinutils.exceptions.NotImplementedException;

public class TypeConstraintSet implements UnifiableType {
	public class Comment implements Comparable<Comment>{
		private final InputPosition pos;
		private final String msg;
		private final Type type;

		public Comment(InputPosition pos, String msg, Type type) {
			this.pos = pos;
			this.msg = msg;
			this.type = type;
		}
		
		@Override
		public String toString() {
			return pos + " - " + msg + (type != null?" " + type:"");
		}

		@Override
		public int compareTo(Comment o) {
			int ret;
			if (pos != null) {
				ret = pos.compareTo(o.pos);
				if (ret != 0)
					return ret;
			}
			ret = msg.compareTo(o.msg);
			if (ret != 0)
				return ret;
			if (type == null)
				return 0;
			ret = type.signature().compareTo(o.type.signature());
			return ret;
		}
	}

	public class UnifiableApplication {
		private final InputPosition pos;
		private final List<Type> args;
		private final Type ret;

		public UnifiableApplication(InputPosition pos, List<Type> args, Type ret) {
			this.pos = pos;
			this.args = args;
			this.ret = ret;
		}

		public PosType asApply() {
			return new PosType(pos, new Apply(args, ret));
		}
	}

	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final InputPosition pos;
	private final String motive;
	private final String id;
	private final Set<PosType> incorporatedBys = new HashSet<>();
	private final Map<StructDefn, StructTypeConstraints> ctors = new TreeMap<>(StructDefn.nameComparator);
	private final Set<PosType> types = new HashSet<>();
	private final Set<UnifiableApplication> applications = new HashSet<>();
	private Type resolvedTo;
	private int usedOrReturned = 0;
	private final TreeSet<Comment> comments = new TreeSet<>();
	
	public TypeConstraintSet(RepositoryReader r, CurrentTCState state, InputPosition pos, String id, String motive) {
		repository = r;
		this.state = state;
		this.pos = pos;
		this.id = id;
		this.motive = motive;
		comments.add(new Comment(pos, id + " created because " + motive, null));
	}

	public boolean isResolved() {
		return resolvedTo != null;
	}
	
	@Override
	public boolean enhance() {
		boolean again = false;
		while (true) {
			List<PosType> refs = new ArrayList<>();
			for (PosType pt : types) {
				Type t = pt.type;
				if (t instanceof TypeConstraintSet) {
					TypeConstraintSet other = (TypeConstraintSet) t;
					boolean needsMe = true;
					for (PosType t2 : other.types) {
						if (t2.type == this) {
							needsMe = false;
						} else if (!types.contains(t2))
							refs.add(t2);
					}
					if (needsMe) {
						other.sameAs(pos, this);
						again = true;
					}
				}
			}
			if (refs.isEmpty())
				return again;
			again = true;
//			System.out.println("Adding " + refs + " to " + asTCS());
			types.addAll(refs);
			for (PosType t : refs) {
				if (t.type instanceof TypeConstraintSet)
					this.comments.addAll(((TypeConstraintSet)t.type).comments);
			}
		}
	}
	
	@Override
	public Type resolve(ErrorReporter errors, boolean hard) {
		if (resolvedTo != null)
			return resolvedTo;
		Set<PosType> tys = new HashSet<>();
		for (PosType pt : types) {
			Type t = pt.type;
			if (t == null)
				throw new NotImplementedException("should not be null");
			if (t instanceof StructDefn && ((StructDefn)t).hasPolys()) {
				StructDefn sd = (StructDefn) t;
				List<Type> polys = new ArrayList<>();
				// TODO: I think for type cases we should in fact insist on them specifying the polymorphic vars
				// We would then have them here (probably already as a PolyInstance!) ...
				for (PolyType p : sd.polys()) {
					polys.add(LoadBuiltins.any);
				}
				tys.add(new PosType(pt.pos, new PolyInstance(pos, sd, polys)));
			} else
				tys.add(pt);
		}

		// We have been explicitly told that these are true, usually through pattern matching
		// This is too broad; but I think we are going to need to do something like this ultimately, so just suck it up ...
		for (Entry<StructDefn, StructTypeConstraints> e : ctors.entrySet()) {
			StructDefn ty = e.getKey();
			if (!ty.hasPolys())
				tys.add(new PosType(pos, ty));
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
				tys.add(new PosType(pos, new PolyInstance(pos, ty, polys)));
			}
		}
		
		if (applications.size() == 1) {
			UnifiableApplication ua = applications.iterator().next();
			tys.add(ua.asApply());
		} else if (!applications.isEmpty()) {
			List<List<PosType>> args = new ArrayList<>();
			List<PosType> ret = new ArrayList<>();
			for (UnifiableApplication x : applications) {
				// I feel like this *could* get us into an infinite loop, but I don't think it actually can on account of how we introduce the return variable
				// and while it could possibly recurse, I don't think that can then refer back to us
				// If we do run into that problem, we should probably throw a special "UnresolvedReferenceException" here and then catch that in the loop
				// and then go around again
				// But at least make sure you have a test case before doing that ...
				int k = 0;
				for (Type t : x.args) {
					if (args.size() == k)
						args.add(new ArrayList<PosType>());
					args.get(k++).add(new PosType(x.pos, t));
				}
				ret.add(new PosType(x.pos, x.ret));
			}
			List<Type> cargs = new ArrayList<>();
			for (List<PosType> a : args)
				cargs.add(state.consolidate(pos, a).type);
			PosType rt = state.consolidate(pos, ret);
			tys.add(new PosType(rt.pos, new Apply(cargs, rt.type)));
		}
		
		tys.addAll(incorporatedBys);
		
		List<UnifiableType> sameAs = new ArrayList<>();
		HashSet<PosType> resolved = new HashSet<>();
		for (PosType ty : tys) {
			if (ty.type instanceof UnifiableType) {
				TypeConstraintSet ut = (TypeConstraintSet) ty.type;
				if (ut.isResolved()) {
					Type res = ut.resolve(errors, true);
					if (res instanceof ErrorType) {
						sameAs.add(ut);
					} else
						resolved.add(new PosType(ut.pos, res));
				} else
					sameAs.add(ut);
			} else
				resolved.add(ty);
		}

		if (resolved.isEmpty()) {
			if (!hard) { // don't resolve just yet ...
				return null;
			}
			if (usedOrReturned > 0 || !sameAs.isEmpty())
				resolvedTo = state.nextPoly(pos);
			else
				resolvedTo = LoadBuiltins.any;
		} else if (resolved.size() == 1)
			resolvedTo = resolved.iterator().next().type;
		else {
			Set<Type> alltys = new HashSet<>();
			for (PosType pt : resolved) {
				if (pt.type instanceof ErrorType)
					return pt.type;
				alltys.add(pt.type);
			}
			resolvedTo = repository.findUnionWith(alltys);
			if (resolvedTo == null && hard) {
				TreeSet<String> msgs = new TreeSet<>();
				for (Type ty : alltys)
					msgs.add(ty.signature());
				for (Comment c : comments) {
					if (c.type != null && !(c.type instanceof UnifiableType)) {
						String msg = "cannot unify types: " + c.msg;
						msg += " " + c.type.signature();
						errors.message(c.pos,  msg);
					}
				}
				resolvedTo = new ErrorType();
			}
		}

		if (resolvedTo == null || resolvedTo instanceof ErrorType)
			return resolvedTo;
		
		for (UnifiableType ut : sameAs) {
			if (!ut.isResolved())
				ut.incorporatedBy(this.pos, resolvedTo);
		}
		
		return resolvedTo;
	}

	@Override
	public void isReturned(InputPosition pos) {
		comments.add(new Comment(pos, "returned", null));
		usedOrReturned++;
	}

	@Override
	public void isUsed(InputPosition pos) {
		comments.add(new Comment(pos, "used", null));
		usedOrReturned++;
	}

	@Override
	public void incorporatedBy(InputPosition pos, Type incorporator) {
		if (incorporator instanceof ErrorType)
			return;
		if (incorporator == null)
			throw new NotImplementedException("incorporator shoud not be null");
		incorporatedBys.add(new PosType(pos, incorporator));
		comments.add(new Comment(pos, "incorporated by", incorporator));
	}

	@Override
	public String signature() {
//		if (resolvedTo == null)
			return asTCS();
//		return resolvedTo.signature();
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
		comments.add(new Comment(pos, "incorporates " + other, other));
		this.isPassed(pos, other);
		return true;
	}

	@Override
	public StructTypeConstraints canBeStruct(InputPosition pos, FunctionName fn, StructDefn sd) {
		comments.add(new Comment(pos, "can be struct " + sd, sd));
		if (!ctors.containsKey(sd))
			ctors.put(sd, new StructFieldConstraints(repository, fn, state, pos, sd));
		return ctors.get(sd);
	}

	@Override
	public void sameAs(InputPosition pos, Type ofType) {
		comments.add(new Comment(pos, "same as", ofType));
		if (ofType == null)
			throw new NotImplementedException("types cannot be null");
		types.add(new PosType(pos, ofType));
		if (ofType instanceof TypeConstraintSet)
			this.comments.addAll(((TypeConstraintSet)ofType).comments);

	}

	@Override
	public void canBeType(InputPosition pos, Type ofType) {
		comments.add(new Comment(pos, "can be", ofType));
		if (ofType == null)
			throw new NotImplementedException("types cannot be null");
		types.add(new PosType(pos, ofType));
	}
	
	@Override
	public UnifiableType canBeAppliedTo(InputPosition pos, List<PosType> args) {
		// Here we introduce a new variable that we will be able to constrain
		StringBuilder motive = new StringBuilder("apply " + id + " to");
		for (PosType t : args) {
			motive.append(" ");
			Type tt = t.type;
			if (tt instanceof TypeConstraintSet)
				motive.append(((TypeConstraintSet)tt).id);
			else
				motive.append(tt.signature());
		}
		UnifiableType ret = state.createUT(pos, motive.toString());
		List<Type> targs = new ArrayList<>();
		for (PosType ty : args) {
			targs.add(ty.type);
			if (ty.type instanceof UnifiableType)
				((UnifiableType)ty.type).isUsed(ty.pos);
		}
		addApplication(pos, targs, ret);
		return ret;
	}

	void consolidatedApplication(Apply a) {
		List<Type> l = new ArrayList<>(a.tys);
		Type ret = l.remove(l.size()-1);
		addApplication(pos, l, ret);
	}
	
	private void addApplication(InputPosition pos, List<Type> args, Type ret) {
		applications.add(new UnifiableApplication(pos, args, ret));
		comments.add(new Comment(pos, "application " + args + " ==> " + ret, ret));
	}

	@Override
	public void determinedType(PosType ofType) {
		if (ofType == null || ofType.type == null)
			throw new NotImplementedException("types cannot be null");
		if (ofType.type instanceof Apply) {
			Apply a = (Apply) ofType.type;
			addApplication(ofType.pos, a.tys.subList(0, a.tys.size()-1), a.tys.get(a.tys.size()-1));
		} else
			types.add(ofType);
	}

	@Override
	public void isPassed(InputPosition loc, Type ai) {
		comments.add(new Comment(pos, "isPassed " + ai, ai));
		// This is the same implementation as "canBeType" - is that correct?
		types.add(new PosType(loc, ai));
	}

	public String asTCS() {
		return "TCS{" + (pos == null? "NULL":pos.inFile()) + ":" + id + (motive != null ? ":" + motive : "") + "}";
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
			return id + ":" + signature();
		else
			return asTCS();
	}
}
