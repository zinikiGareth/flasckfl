package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.graphs.DirectedAcyclicGraph;

public class TypeConstraintSet implements UnifiableType {
	private final static Logger logger = LoggerFactory.getLogger("TCUnification");

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
	private final Map<NamedType, StructTypeConstraints> ctors = new TreeMap<>(NamedType.nameComparator);
	private final Set<PosType> types = new HashSet<>();
	private final Set<UnifiableApplication> applications = new HashSet<>();
	private Type resolvedTo;
	private int usedOrReturned = 0;
	private final TreeSet<Comment> comments = new TreeSet<>();
	private final Set<PosType> tys = new HashSet<>();
	private TypeConstraintSet redirectedTo;
	private Set<UnifiableType> acquired = new HashSet<>();
	private Set<UnifiableType> polyvars = new HashSet<>();
	private Comparator<Type> signatureComparator = new Comparator<Type>() {
		@Override
		public int compare(Type o1, Type o2) {
			return o1.signature().compareTo(o2.signature());
		}
	};
	final static Comparator<? super PosType> posNameComparator = new Comparator<PosType>() {

		@Override
		public int compare(PosType o1, PosType o2) {
			int cp = o1.pos.compareTo(o2.pos);
			if (cp != 0)
				return cp;
			
			return o1.type.toString().compareTo(o2.type.toString());
		}
	};
	
	public TypeConstraintSet(RepositoryReader r, CurrentTCState state, InputPosition pos, String id, String motive) {
		repository = r;
		this.state = state;
		this.pos = pos;
		this.id = id;
		this.motive = motive;
		comments.add(new Comment(pos, id + " created because " + motive, null));
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String motive() {
		return motive;
	}
	
	@Override
	public Type resolvedTo() {
		if (redirectedTo != null)
			return redirectedTo.resolvedTo();
		if (resolvedTo == null)
			throw new InvalidUsageException("wait until " + id + " is resolved");
		return resolvedTo;
	}
	
	private Type resolvingTo(HashSet<UnifiableType> workingOn) {
		if (redirectedTo != null)
			return redirectedTo.resolvingTo(workingOn);
		if (resolvedTo != null)
			return resolvedTo;
		if (workingOn.contains(this))
			return null;
		throw new InvalidUsageException("wait until " + id + " is resolved");
	}

	@Override
	public boolean mustBeMessage(ErrorReporter errors) {
		if (resolvedTo != null) {
			return EnsureListMessage.validate(errors, pos, resolvedTo);
		}
		throw new NotImplementedException();
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
			types.addAll(refs);
			for (PosType t : refs) {
				if (t.type instanceof TypeConstraintSet)
					this.comments.addAll(((TypeConstraintSet)t.type).comments);
			}
		}
	}

	@Override
	public void acquireOthers(List<UnifiableType> considered) {
		// What happens if this list contains multiple?
		// And some have been considered and others haven't?
		// And what if two have both not been redirected?
		HashSet<PosType> ts1 = new HashSet<>(types);
		ts1.addAll(incorporatedBys);
		for (PosType pt : ts1) {
			Type t = pt.type;
			if (t instanceof TypeConstraintSet) {
				TypeConstraintSet ut = (TypeConstraintSet) t;
				if (considered.contains(ut)) {
					// be acquired by that
					ut.redirectedTo().acquire(this);
				} else
					acquire(ut);
			}
		}
		
		// Now forget about them
		Iterator<PosType> i = types.iterator();
		while (i.hasNext()) {
			if (i.next().type instanceof TypeConstraintSet)
				i.remove();
		}
	}
	
	public UnifiableType redirectedTo() {
		if (redirectedTo != null)
			return redirectedTo.redirectedTo();
		else
			return this;
	}

	@Override
	public boolean isRedirected() {
		return redirectedTo != null;
	}
	
	public void acquire(UnifiableType ut) {
		if (ut == this)
			return;
		TypeConstraintSet tcs = (TypeConstraintSet)ut;
		if (tcs.redirectedTo != null) {
			acquire(tcs.redirectedTo);
			return;
		}
		tcs.redirectedTo = this;
		this.applications.addAll(tcs.applications);
		this.comments.addAll(tcs.comments);
		this.ctors.putAll(tcs.ctors);
		this.incorporatedBys.addAll(tcs.incorporatedBys);
		for (PosType ty : tcs.types)
			if (!(ty.type instanceof UnifiableType))
				this.types.add(ty);
		this.usedOrReturned += tcs.usedOrReturned;
		this.acquired.add(ut);
	}

	
	@Override
	public void expandUnions() {
		List<PosType> addMore = new ArrayList<>();
		for (PosType pt : types) {
			expandUnion(addMore, pt);
		}
		types.addAll(addMore);
		addMore = new ArrayList<>();
		for (PosType pt : incorporatedBys) {
			expandUnion(addMore, pt);
		}
		incorporatedBys.addAll(addMore);
	}

	private void expandUnion(List<PosType> addMore, PosType pt) {
		InputPosition pos = pt.pos;
		Type t = pt.type;
		if (t instanceof UnionTypeDefn) {
			UnionTypeDefn utd = (UnionTypeDefn) t;
			for (TypeReference c : utd.cases) {
				StructDefn sd = (StructDefn) c.defn();
				addMore.add(new PosType(pos, sd));
			}
		} else if (t instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) t;
			if (pi.struct() instanceof UnionTypeDefn) {
				UnionTypeDefn utd = (UnionTypeDefn) pi.struct();
				Map<PolyType, Type> mt = new HashMap<>();
				for (int p=0;p<utd.polys().size();p++) {
					mt.put(utd.polys().get(p), pi.getPolys().get(p));
				}
				for (TypeReference c : utd.cases) {
					if (c.defn() instanceof StructDefn) {
						StructDefn sd = (StructDefn) c.defn();
						if (sd.hasPolys())
							throw new CantHappenException("should be polyinstance");
						addMore.add(new PosType(pos, sd));
					} else if (c.defn() instanceof PolyInstance) {
						PolyInstance pc = (PolyInstance) c.defn();
						List<Type> pm = new ArrayList<>();
						for (Type p : pc.getPolys())
							pm.add(mt.get(p));
						addMore.add(new PosType(pos, new PolyInstance(pos, pc.struct(), pm)));
					} else
						throw new HaventConsideredThisException("expecting struct or polyinstance");
				}
			}
		}
	}

	@Override
	public void mergePolyVars() {
		ListMap<PolyHolder, PolyInstance> groups = new ListMap<>();
		for (PosType pt : types) {
			if (pt.type instanceof PolyInstance) {
				PolyInstance pi = (PolyInstance) pt.type;
				if (pi.struct() instanceof PolyHolder) // You would think, but Tuples are (perhaps incorrectly) an exception to this rule
					groups.add((PolyHolder) pi.struct(), pi);
			}
		}
		for (PosType pt : incorporatedBys) {
			if (pt.type instanceof PolyInstance) {
				PolyInstance pi = (PolyInstance) pt.type;
				if (pi.struct() instanceof PolyHolder) // You would think, but Tuples are (perhaps incorrectly) an exception to this rule
					groups.add((PolyHolder) pi.struct(), pi);
			}
		}
		for (NamedType nt : ctors.keySet()) {
			if (nt instanceof PolyInstance) {
				PolyInstance pi = (PolyInstance) nt;
				if (pi.struct() instanceof PolyHolder) // You would think, but Tuples are (perhaps incorrectly) an exception to this rule
					groups.add((PolyHolder) pi.struct(), pi);
			}
		}
		for (PolyHolder e : groups.keySet()) {
			List<PolyInstance> list = groups.get(e);
			if (list.size() > 1) {
				for (int i=0;i<e.polys().size();i++) {
					List<PosType> tojoin = new ArrayList<>();
					for (PolyInstance pi : list) {
						tojoin.add(new PosType(pi.location(), pi.getPolys().get(i)));
					}
					state.consolidate(tojoin.get(0).location(), tojoin);
				}
			}
		}
	}

	public void collectInfo(ErrorReporter errors, DirectedAcyclicGraph<UnifiableType> dag) {
		logger.debug("collecting info on " + id + ": " + motive);
		HashSet<PosType> ts1 = new HashSet<>(types);
		ts1.addAll(incorporatedBys);
		for (PosType pt : ts1) {
			Type t = pt.type;
			logger.debug("  have type " + t);
			if (t == null)
				throw new NotImplementedException("should not be null");
			
			if (t instanceof UnifiableType) {
				UnifiableType ut = ((UnifiableType) t).redirectedTo();
				dag.ensure(ut);
				if (this != ut)
					dag.ensureLink(this, ut);
			}
			if (t instanceof PolyInstance) {
				PolyInstance pi = (PolyInstance) t;
				linkToPVs(dag, pi);
				tys.add(pt);	
			} else if (t instanceof StructDefn && ((StructDefn)t).hasPolys()) {
				StructDefn sd = (StructDefn) t;
				List<Type> polys = new ArrayList<>();
				// TODO: I think for type cases we should in fact insist on them specifying the polymorphic vars
				// We would then have them here (probably already as a PolyInstance!) ...
				for (PolyType p : sd.polys()) {
					polys.add(LoadBuiltins.any);
				}
				tys.add(new PosType(pt.pos, new PolyInstance(pos, sd, polys)));
			} else if (t instanceof EnsureListMessage) {
				EnsureListMessage elm = (EnsureListMessage) t;
				if (elm.checking() instanceof UnifiableType) {
					UnifiableType ut = ((UnifiableType) elm.checking()).redirectedTo();
					dag.ensure(ut);
					dag.ensureLink(this, ut);
				}
				tys.add(pt);
			} else
				tys.add(pt);
		}

		// We have been explicitly told that these are true, usually through pattern matching
		// This is too broad; but I think we are going to need to do something like this ultimately, so just suck it up ...
		for (Entry<NamedType, StructTypeConstraints> e : ctors.entrySet()) {
			NamedType ty = e.getKey();
			logger.debug("  have ctor " + ty);
			if (ty instanceof StructDefn && !((StructDefn)ty).hasPolys())
				tys.add(new PosType(pos, ty));
			else if (ty instanceof PolyInstance) {
				// I think it may be possible to simplify this by just going after the polyinstance args directly
				StructDefn sd = (StructDefn) ((PolyInstance) ty).struct();
				StructTypeConstraints stc = ctors.get(ty);
				Map<PolyType, Type> polyMap = new HashMap<>();
				for (StructField f : stc.fields()) {
					PolyType pt = sd.findPoly(f.type);
					if (pt == null)
						continue;
					dag.ensure(stc.get(f).redirectedTo());
					dag.ensureLink(this, stc.get(f).redirectedTo());
					polyMap.put(pt, stc.get(f));
				}
				List<Type> polys = new ArrayList<>();
				for (PolyType p : sd.polys()) {
					if (polyMap.containsKey(p))
						polys.add(polyMap.get(p));
					else
						polys.add(LoadBuiltins.any);
				}
				tys.add(new PosType(pos, new PolyInstance(pos, sd, polys)));
			}
		}
		
		if (applications.size() == 1) {
			UnifiableApplication ua = applications.iterator().next();
			tys.add(ua.asApply());
			for (Type t : ua.args) {
				if (t instanceof UnifiableType) {
					UnifiableType ut = ((UnifiableType) t).redirectedTo();
					dag.ensure(ut);
					dag.ensureLink(this, ut);
				}
			}
		} else if (!applications.isEmpty()) {
			List<Set<PosType>> args = new ArrayList<>();
			Set<PosType> ret = new TreeSet<>(posNameComparator);
			for (UnifiableApplication x : applications) {
				// I feel like this *could* get us into an infinite loop, but I don't think it actually can on account of how we introduce the return variable
				// and while it could possibly recurse, I don't think that can then refer back to us
				// If we do run into that problem, we should probably throw a special "UnresolvedReferenceException" here and then catch that in the loop
				// and then go around again
				// But at least make sure you have a test case before doing that ...
				int k = 0;
				for (Type t : x.args) {
					if (args.size() == k)
						args.add(new TreeSet<PosType>(posNameComparator));
					args.get(k++).add(new PosType(x.pos, t));
				}
				ret.add(new PosType(x.pos, x.ret));
			}
			List<Type> cargs = new ArrayList<>();
			for (Set<PosType> a : args) {
				Type ct = state.collapse(pos, a).type;
				if (ct instanceof UnifiableType) {
					UnifiableType ut = ((UnifiableType) ct).redirectedTo();
					dag.ensure(ut);
					dag.ensureLink(this, ut);
				}
				cargs.add(ct);
			}
			PosType rt = state.collapse(pos, ret);
			if (rt.type instanceof UnifiableType) {
				UnifiableType ut = ((UnifiableType) rt.type).redirectedTo();
				dag.ensure(ut);
				dag.ensureLink(this, ut);
			}
			tys.add(new PosType(rt.pos, new Apply(cargs, rt.type)));
		}
		
		for (UnifiableType pv : polyvars) {
			dag.ensure(pv);
			dag.ensureLink(this, pv);
		}
	}

	private void linkToPVs(DirectedAcyclicGraph<UnifiableType> dag, PolyInstance pi) {
		for (Type pv : pi.getPolys()) {
			if (pv instanceof UnifiableType) {
				UnifiableType ut = ((UnifiableType) pv).redirectedTo();
				dag.ensure(ut);
				dag.ensureLink(this, ut);
			} else if (pv instanceof PolyInstance) {
				linkToPVs(dag, (PolyInstance) pv);
			}
		}
	}

	@Override
	public Type resolve(ErrorReporter errors) {
		logger.debug("resolving " + this.id + " " + this.motive + " types = " + tys);

		if (redirectedTo != null && redirectedTo.resolvedTo == null)
			throw new CantHappenException("We shouldn't be asked before our redirection");
		if (redirectedTo != null)
			return redirectedTo.resolvedTo;
		if (resolvedTo != null)
			return resolvedTo;
		
		HashSet<PosType> resolved = new HashSet<>();
		for (PosType ty : tys) {
			if (ty.type != this) {
				Type rt = resolvePolyArg(new HashSet<UnifiableType>(), ty.type);
				if (rt != null) {
					if (rt instanceof EnsureListMessage) {
						Type mt = ((EnsureListMessage)rt).validate(errors);
						resolved.add(new PosType(ty.pos, mt));
					} else
						resolved.add(new PosType(ty.pos, rt));
				}
			}
		}

		if (resolved.isEmpty()) {
			if (usedOrReturned > 0 || !acquired.isEmpty())
				resolvedTo = state.nextPoly(pos);
			else
				resolvedTo = LoadBuiltins.any;
		} else if (resolved.size() == 1)
			resolvedTo = resolved.iterator().next().type;
		else {
			Set<Type> alltys = new TreeSet<>(signatureComparator);
			for (PosType pt : resolved) {
				if (pt.type instanceof ErrorType) {
					resolvedTo = pt.type;
					return pt.type;
				}
				alltys.add(pt.type);
			}
			resolvedTo = repository.findUnionWith(alltys);
			if (resolvedTo == null) {
				logger.info("could not unify " + this.id);
				TreeSet<String> msgs = new TreeSet<>();
				for (Type ty : alltys)
					msgs.add(ty.signature());
				for (Comment c : comments) {
					if (c.type != null && !(c.type instanceof UnifiableType)) {
						String msg = "  has contraint: '" + c.msg + "'";
						msg += " " + c.type.signature();
						logger.info(msg);
					}
				}
				TreeSet<String> tyes = new TreeSet<String>();
				for (Type ty : alltys)
					tyes.add(ty.signature());
				errors.message(pos, "cannot unify " + tyes);
				resolvedTo = new ErrorType();
			}
		}

		logger.debug("resolved to " + resolvedTo);
		return resolvedTo;
	}

	private Type resolvePolyArg(HashSet<UnifiableType> workingOn, Type ty) {
		workingOn.add(this);
		if (ty instanceof UnifiableType) {
			return ((TypeConstraintSet)ty).resolvingTo(workingOn);
		} else if (ty instanceof PolyInstance) {
			return resolvePolyArgs(workingOn, (PolyInstance)ty);
		} else
			return ty;
	}

	private Type resolvePolyArgs(HashSet<UnifiableType> workingOn, PolyInstance pi) {
		List<Type> rps = new ArrayList<Type>();
		for (Type t : pi.getPolys()) {
			Type curr = resolvePolyArg(workingOn, t);
			if (curr != null)
				rps.add(curr);
		}
		return new PolyInstance(pi.location(), pi.struct(), rps);
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

	public void hasPolyVar(InputPosition pos, UnifiableType pv) {
		comments.add(new Comment(pos, "has poly var " + pv.id(), null));
		polyvars.add(pv);
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
		if (resolvedTo == null)
			return id; // asTCS();
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
		comments.add(new Comment(pos, "incorporates " + other, other));
		this.isPassed(pos, other);
		return true;
	}

	@Override
	public StructTypeConstraints canBeStruct(InputPosition pos, FunctionName fn, StructDefn sd) {
		comments.add(new Comment(pos, "can be struct " + sd, sd));
		if (!ctors.containsKey(sd)) {
			StructFieldConstraints sfc = new StructFieldConstraints(repository, fn, state, this, pos, sd);
			ctors.put(sfc.polyInstance(), sfc);
		}
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
			if (tt instanceof UnifiableType)
				motive.append(((UnifiableType)tt).id());
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
		return "TCS" + (isRedirected()?"*"+redirectedTo.id()+"*":"") + "{" + (pos == null? "NULL":pos.inFile()) + ":" + id + (motive != null ? ":" + motive : "") + "}";
	}

	public String debugInfo() {
		StringBuilder ret = new StringBuilder();
		ret.append(asTCS());
		ret.append(" =>");
		if (this.resolvedTo != null) {
			ret.append(" ");
			ret.append("{");
			ret.append(resolvedTo);
			ret.append("}");
		}
		if (!tys.isEmpty()) {
			showTys(ret, tys);
		} else {
			showTys(ret, types);
			showTys(ret, incorporatedBys);
			showCtors(ret, ctors);
		}
		return ret.toString();
	}
	
	private void showTys(StringBuilder ret, Set<PosType> pts) {
		for (PosType pt : pts) {
			ret.append(" ");
			showType(ret, pt.type);
		}
	}

	private void showType(StringBuilder ret, Type t) {
		if (t instanceof UnifiableType) {
			ret.append(((UnifiableType)t).id());
		} else if (t instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) t;
			ret.append(pi.struct().signature());
			String sep = "[";
			for (Type i : pi.getPolys()) {
				ret.append(sep);
				showType(ret, i);
				sep = ",";
			}
			ret.append("]");
		} else
			ret.append(t.signature());
	}

	private void showCtors(StringBuilder ret, Map<NamedType, StructTypeConstraints> ct) {
		for (Entry<NamedType, StructTypeConstraints> e : ct.entrySet()) {
			ret.append(" $");
			showType(ret, e.getKey());
		}
	}

	@Override
	public String toString() {
		if (resolvedTo != null)
			return id + ":" + signature();
		else if (((TypeConstraintSet)redirectedTo()).resolvedTo != null) {
			TypeConstraintSet rt = (TypeConstraintSet)redirectedTo();
			return id + "*" + rt.id + ":" + rt.resolvedTo; 
		} else
			return asTCS();
	}
}
