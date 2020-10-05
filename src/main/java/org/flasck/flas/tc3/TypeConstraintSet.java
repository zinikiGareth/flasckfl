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
import java.util.function.Function;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AccessorHolder;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
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
			if (args.isEmpty())
				return new PosType(pos, ret);
			return new PosType(pos, new Apply(args, ret));
		}

		@Override
		public String toString() {
			return args.toString() + "->" + ret;
		}
	}

	public class ErrorConstraint {
		private final Function<Type, Boolean> predicate;
		private final InputPosition location;
		private final String err;
		private ErrorConstraint chain;

		public ErrorConstraint(Function<Type, Boolean> predicate, InputPosition pos, String err) {
			this.predicate = predicate;
			location = pos;
			this.err = err;
		}
		
		public void apply(ErrorReporter errors, Type t) {
			if (!predicate.apply(t))
				errors.message(location, err);
			else if (chain != null)
				chain.apply(errors, t);
		}

		public ErrorConstraint chain(ErrorConstraint errorConstraint) {
			this.chain = errorConstraint;
			return errorConstraint;
		}
	}
	
	public class FieldOf {
		private final MemberExpr fieldExpr;
		private final UnifiableType fieldOf;
		private final String fieldName;

		public FieldOf(MemberExpr fieldExpr, UnifiableType fieldOf, String fieldName) {
			this.fieldExpr = fieldExpr;
			this.fieldOf = fieldOf;
			this.fieldName = fieldName;
		}
	}

	private final static Logger logger = LoggerFactory.getLogger("TCUnification");
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
	private final Set<UnifiableType> acquired = new HashSet<>();
	private final Set<UnifiableType> polyvars = new HashSet<>();
	private final List<ErrorConstraint> errorConstraints = new ArrayList<ErrorConstraint>();
	private Comparator<Type> signatureComparator = new Comparator<Type>() {
		@Override
		public int compare(Type o1, Type o2) {
			return o1.signature().compareTo(o2.signature());
		}
	};
	private final boolean needAll;
	private final List<CallOnResolution> onResolved = new ArrayList<>();
	private List<FieldOf> fieldExprs = new ArrayList<>();
	final static Comparator<? super PosType> posNameComparator = new Comparator<PosType>() {

		@Override
		public int compare(PosType o1, PosType o2) {
			if (o1.pos == null) {
				if (o2.pos != null)
					return -1;
			} else if (o2.pos == null) {
				return 1;
			} else {
				int cp = o1.pos.compareTo(o2.pos);
				if (cp != 0)
					return cp;
			}
			
			return o1.type.toString().compareTo(o2.type.toString());
		}
	};
	
	public TypeConstraintSet(RepositoryReader r, CurrentTCState state, InputPosition pos, String id, String motive, boolean unionNeedsAll) {
		repository = r;
		this.state = state;
		this.pos = pos;
		this.id = id;
		this.motive = motive;
		this.needAll = unionNeedsAll;
		comments.add(new Comment(pos, id + " created because " + motive, null));
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public InputPosition location() {
		return pos;
	}
	
	@Override
	public String motive() {
		return motive;
	}
	
	@Override
	public Type resolvedTo() {
		if (redirectedTo != null)
			return redirectedTo().resolvedTo();
		if (resolvedTo == null)
			throw new InvalidUsageException("wait until " + id + " is resolved");
		return resolvedTo;
	}
	
	private Type resolvingTo(HashSet<UnifiableType> workingOn) {
		if (redirectedTo != null)
			return ((TypeConstraintSet) redirectedTo()).resolvingTo(workingOn);
		if (resolvedTo != null)
			return resolvedTo;
		if (workingOn.contains(this))
			return null;
		throw new InvalidUsageException("wait until " + id + " is resolved: working on " + workingOn);
	}

	@Override
	public boolean mustBeMessage(ErrorReporter errors) {
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
		if (isRedirected()) {
			redirectedTo().acquire(ut);
			return;
		}
		TypeConstraintSet tcs = (TypeConstraintSet)ut;
		if (tcs.redirectedTo != null) {
			acquire(tcs.redirectedTo());
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
		this.acquired.add(tcs);
		this.acquired.addAll(tcs.acquired);
		this.errorConstraints.addAll(tcs.errorConstraints);
		this.onResolved.addAll(tcs.onResolved);
		this.fieldExprs.addAll(tcs.fieldExprs);
	}

	@Override
	public void expandUsed() {
		if (usedOrReturned == 0)
			return;
		for (PosType pt : types) {
			expandUsage(pt.type);
		}
		for (StructTypeConstraints e : ctors.values()) {
			for (StructField sf : e.fields())
				expandUsage(e.get(sf));
		}
		for (PosType pt : incorporatedBys) {
			expandUsage(pt.type);
		}
	}
	
	private void expandUsage(Type type) {
		if (type instanceof TypeConstraintSet) {
			TypeConstraintSet ut = (TypeConstraintSet)type;
			if (ut.usedOrReturned == 0) {
				ut.usedOrReturned = 1;
				ut.expandUsed();
			}
		} else if (type instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) type;
			for (Type e : pi.polys())
				expandUsage(e);
		}
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
				Type sd = (Type) c.defn();
				addMore.add(new PosType(pos, sd));
			}
		} else if (t instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) t;
			if (pi.struct() instanceof UnionTypeDefn) {
				UnionTypeDefn utd = (UnionTypeDefn) pi.struct();
				Map<PolyType, Type> mt = new HashMap<>();
				for (int p=0;p<utd.polys().size();p++) {
					mt.put(utd.polys().get(p), pi.polys().get(p));
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
						for (Type p : pc.polys())
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
						tojoin.add(new PosType(pi.location(), pi.polys().get(i)));
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
				for (@SuppressWarnings("unused") PolyType p : sd.polys()) {
					polys.add(LoadBuiltins.any);
				}
				tys.add(new PosType(pt.pos, new PolyInstance(pos, sd, polys)));
			} else if (t instanceof Apply) {
				Apply a = (Apply) t;
				for (Type t1 : a.tys) {
					if (t1 instanceof PolyInstance)
						linkToPVs(dag, (PolyInstance) t1);
					else if (t1 instanceof UnifiableType) {
						UnifiableType ut = (UnifiableType) t1;
						dag.ensure(ut);
						dag.ensureLink(this, ut);
					}
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
			if (ty instanceof StructDefn && !((StructDefn)ty).hasPolys()) {
				tys.add(new PosType(pos, ty));
			} else if (ty instanceof PolyInstance) {
				linkPVs(dag, ty);
				tys.add(new PosType(pos, ty));
			}
		}
		
		if (applications.size() == 1) {
			UnifiableApplication ua = applications.iterator().next();
			tys.add(ua.asApply());
			logger.debug("  [have 1 application: " + ua + "]");
			for (Type t : ua.args) {
				if (t instanceof UnifiableType) {
					UnifiableType ut = ((UnifiableType) t).redirectedTo();
					logger.debug("  have apply dependency on: " + ut.id());
					dag.ensure(ut);
					dag.ensureLink(this, ut);
				} else if (t instanceof PolyInstance) {
					PolyInstance pi = (PolyInstance) t;
					for (Type ti : pi.polys()) {
						if (ti instanceof UnifiableType) {
							UnifiableType ut = ((UnifiableType)ti).redirectedTo();
							dag.ensure(ut);
							dag.ensureLink(this, ut);
						}
					}
				}
			}
			if (ua.ret instanceof UnifiableType) {
				UnifiableType ut = ((UnifiableType)ua.ret).redirectedTo();
				logger.debug("  have apply dependency on: " + ut.id());
				dag.ensure(ut);
				dag.ensureLink(this, ut);
			} else if (ua.ret instanceof PolyInstance) {
				PolyInstance pi = (PolyInstance) ua.ret;
				for (Type t : pi.polys()) {
					if (t instanceof UnifiableType) {
						UnifiableType ut = ((UnifiableType)t).redirectedTo();
						dag.ensure(ut);
						dag.ensureLink(this, ut);
					}
				}
			}
		} else if (!applications.isEmpty()) {
			int cnt = Integer.MAX_VALUE;
			for (UnifiableApplication x : applications) {
				cnt = Math.min(cnt, x.args.size());
			}
			List<Set<PosType>> args = new ArrayList<>();
			for (int i=0;i<cnt;i++)
				args.add(new TreeSet<PosType>(posNameComparator));
			for (UnifiableApplication x : applications) {
				logger.debug("  [have application: " + x + "]");
				// I feel like this *could* get us into an infinite loop, but I don't think it actually can on account of how we introduce the return variable
				// and while it could possibly recurse, I don't think that can then refer back to us
				// If we do run into that problem, we should probably throw a special "UnresolvedReferenceException" here and then catch that in the loop
				// and then go around again
				// But at least make sure you have a test case before doing that ...
				int k = 0;
				for (Type t : x.args) {
					if (k >= args.size())
						break;
					args.get(k++).add(new PosType(x.pos, t));
					if (t instanceof UnifiableType) {
						UnifiableType ut = ((UnifiableType) t).redirectedTo();
						logger.debug("  have apply dependency on : " + ut.id());
						dag.ensure(ut);
						dag.ensureLink(this, ut);
					} else if (t instanceof PolyInstance) {
						PolyInstance pi = (PolyInstance) t;
						for (Type ti : pi.polys()) {
							if (ti instanceof UnifiableType) {
								UnifiableType ut = ((UnifiableType)ti).redirectedTo();
								dag.ensure(ut);
								dag.ensureLink(this, ut);
							}
						}
					}
				}
			}
			List<Type> cargs = new ArrayList<>();
			for (Set<PosType> a : args) {
				Type ct = state.consolidate(pos, a).type;
				if (ct instanceof UnifiableType) {
					UnifiableType ut = ((UnifiableType) ct).redirectedTo();
					dag.ensure(ut);
					dag.ensureLink(this, ut);
				}
				cargs.add(ct);
			}
			Set<PosType> ret = new TreeSet<>(posNameComparator);
			for (UnifiableApplication x : applications) {
				Type r = x.ret;
				if (x.args.size() > cnt) {
					List<Type> as = new ArrayList<>();
					for (int i=cnt;i<x.args.size();i++)
						as.add(x.args.get(i));
					r = new Apply(as, r);
				}
				ret.add(new PosType(x.pos, r));
				if (r instanceof UnifiableType) {
					UnifiableType ut = ((UnifiableType)r).redirectedTo();
					logger.debug("  have apply dependency on: " + ut.id());
					dag.ensure(ut);
					dag.ensureLink(this, ut);
				} else if (r instanceof PolyInstance) {
					PolyInstance pi = (PolyInstance) r;
					for (Type t : pi.polys()) {
						if (t instanceof UnifiableType) {
							UnifiableType ut = ((UnifiableType)t).redirectedTo();
							dag.ensure(ut);
							dag.ensureLink(this, ut);
						}
					}
				}
			}
			PosType rt = state.consolidate(pos, ret);
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
		
		if (usedOrReturned > 0)
			logger.info("  is used or returned");

		for (FieldOf ff : this.fieldExprs) {
			logger.debug("  is field '" + ff.fieldName + "' of " + ff.fieldOf.redirectedTo().id());
			dag.ensure(ff.fieldOf.redirectedTo());
			dag.ensureLink(this, ff.fieldOf.redirectedTo());
		}
		
		if (redirectedTo != null) {
			dag.ensure(redirectedTo);
			dag.ensureLink(this, redirectedTo);
		}
	}

	private void linkPVs(DirectedAcyclicGraph<UnifiableType> dag, Type pv) {
		if (pv instanceof UnifiableType) {
			UnifiableType ut = ((UnifiableType) pv).redirectedTo();
			dag.ensure(ut);
			dag.ensureLink(this, ut);
		} else if (pv instanceof PolyInstance) {
			linkToPVs(dag, (PolyInstance) pv);
		}
	}
	
	private void linkToPVs(DirectedAcyclicGraph<UnifiableType> dag, PolyInstance pi) {
		for (Type pv : pi.polys()) {
			linkPVs(dag, pv);
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
					resolved.add(new PosType(ty.pos, rt));
				}
			}
		}
		for (FieldOf ff : fieldExprs) {
			Type r = ff.fieldOf.resolvedTo();
			if (r instanceof PolyInstance) {
				PolyInstance pi = (PolyInstance) r;
				r = pi.struct();
			}
			if (r instanceof ErrorType) {
				resolvedTo = r;
				return r;
			}
			if (r instanceof ObjectDefn) {
				ObjectDefn od = (ObjectDefn) r;
				ObjectMethod meth = od.getMethod(ff.fieldName);
				if (meth != null) {
					resolved.add(new PosType(pos, meth.type()));
					continue;
				}
			}
			if (r instanceof AccessorHolder) {
				AccessorHolder ah = (AccessorHolder) r;
				FieldAccessor f = ah.getAccessor(ff.fieldName);
				if (f != null) {
					resolved.add(new PosType(pos, f.type()));
					continue;
				}
			}
			
			if (r instanceof Primitive) {
				errors.message(pos, "cannot extract field " + ff.fieldName + " from primitive type " + r.signature());
			} else if (r instanceof UnionTypeDefn) {
				errors.message(pos, "cannot access members of unions");
			} else
				errors.message(pos, "there is no field " + ff.fieldName + " in " + r.signature());
		}

		if (resolved.isEmpty()) {
			if (usedOrReturned > 0 || !acquired.isEmpty())
				resolvedTo = state.nextPoly(pos);
			else
				resolvedTo = LoadBuiltins.any;
		} else if (resolved.size() == 1)
			resolvedTo = resolved.iterator().next().type;
		else {
			logger.debug("want to unify " + resolved);
			Set<Type> alltys = new TreeSet<>(signatureComparator);
			Set<Integer> acs = new HashSet<>();
			for (PosType pt : resolved) {
				if (pt.type instanceof ErrorType) {
					resolvedTo = pt.type;
					return pt.type;
				}
				alltys.add(pt.type);
				if (pt.type instanceof Apply)
					acs.add(((Apply)pt.type).argCount());
				else
					acs.add(0);
			}
			logger.debug("acs = " + acs);
			if (acs.size() != 1) {
				// cannot unify functions of different arities
				// or functions with constants
				logger.error("different apply sizes");
				for (PosType pt : resolved)
					logger.error("  " + pt.type + " @ " + pt.pos);
			} else {
				Integer cnt = acs.iterator().next();
				if (cnt != 0) {
					logger.debug("unifying " + alltys);
					List<Type> us = new ArrayList<>();
					for (int i=0;i<=cnt;i++) {
						Set<Type> ms = new HashSet<>();
						for (Type t : alltys) {
							ms.add(((Apply)t).tys.get(i));
						}
						Type rt = repository.findUnionWith(errors, pos, ms, needAll);
						if (rt == null)
							break;
						us.add(rt);
					}
					if (us.size() == cnt+1) {
						resolvedTo = new Apply(us);
					}
				} else {
					logger.debug("looking for union with " + alltys + (needAll?" (need all)":" (accept subset)"));
					resolvedTo = repository.findUnionWith(errors, pos, alltys, needAll);
				}
			}
			if (resolvedTo == null) {
				logger.info("could not unify " + this.id);
				TreeSet<String> tyes = new TreeSet<String>();
				TreeSet<InputPosition> locs = new TreeSet<>();
				boolean alreadyError = false;
				for (PosType ty : resolved) {
					alreadyError |= containsError(ty.type);
					tyes.add(ty.type.signature());
					if (ty.pos != null && ty.pos != LoadBuiltins.pos && ty.pos != Apply.unknown)
						locs.add(ty.pos);
				}
				if (!alreadyError)
					errors.message(pos, locs, "cannot unify " + tyes);
				resolvedTo = new ErrorType();
			}
		}

		for (ErrorConstraint e : errorConstraints)
			e.apply(errors, resolvedTo);
		
		for (FieldOf ff : fieldExprs) {
			ff.fieldExpr.bindContainerType(ff.fieldOf.redirectedTo().resolvedTo());
			ff.fieldExpr.bindContainedType(resolvedTo);
		}
		
		logger.debug("resolved to " + resolvedTo);
		return resolvedTo;
	}

	private boolean containsError(Type type) {
		if (type instanceof ErrorType)
			return true;
		else if (type instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) type;
			for (Type t : pi.polys())
				if (containsError(t))
					return true;
			return false;
		} else
			return false;
	}

	@Override
	public void afterResolution(ErrorReporter errors) {
		for (CallOnResolution c : onResolved)
			c.typeResolved(resolvedTo);
	}

	private Type resolvePolyArg(HashSet<UnifiableType> workingOn, Type ty) {
		workingOn.add(this);
		if (ty instanceof UnifiableType) {
			return ((TypeConstraintSet)ty).resolvingTo(workingOn);
		} else if (ty instanceof PolyInstance) {
			return resolvePolyArgs(workingOn, (PolyInstance)ty);
		} else if (ty instanceof Apply) {
			return resolvePolyArgs(workingOn, (Apply)ty);
		} else
			return ty;
	}

	private Type resolvePolyArgs(HashSet<UnifiableType> workingOn, PolyInstance pi) {
		List<Type> rps = new ArrayList<Type>();
		for (Type t : pi.polys()) {
			Type curr = resolvePolyArg(workingOn, t);
			if (curr != null)
				rps.add(curr);
		}
		return new PolyInstance(pi.location(), pi.struct(), rps);
	}

	private Type resolvePolyArgs(HashSet<UnifiableType> workingOn, Apply ty) {
		List<Type> rps = new ArrayList<Type>();
		for (Type t : ty.tys) {
			Type curr = resolvePolyArg(workingOn, t);
			if (curr == null)
				throw new CantHappenException("resolved type to null");
			rps.add(curr);
		}
		return new Apply(rps);
	}

	@Override
	public void isFieldOf(MemberExpr expr, UnifiableType container, String field) {
		fieldExprs.add(new FieldOf(expr, container, field));
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
		logger.debug(id + ": can be applied to " + args + " returning " + ret);
		addApplication(pos, targs, ret);
		return ret;
	}

	void consolidatedApplication(InputPosition pos, Apply a) {
		List<Type> l = new ArrayList<>(a.tys);
		Type ret = l.remove(l.size()-1);
		logger.debug(id + ": have consolidated application " + l + " returning " + ret);
		addApplication(pos, l, ret);
	}
	
	public void recordApplication(InputPosition pos, List<Type> args) {
		logger.debug(id + ": recording application " + args);
		ArrayList<Type> a2 = new ArrayList<>(args);
		Type ret = a2.remove(args.size()-1);
		addApplication(pos, a2, ret);
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
			List<Type> args = a.tys.subList(0, a.tys.size()-1);
			Type ret = a.tys.get(a.tys.size()-1);
			logger.debug(id + " has apply type: " + args + " returning " + ret);
			addApplication(ofType.pos, args, ret);
		} else
			types.add(ofType);
	}

	@Override
	public void isPassed(InputPosition loc, Type ai) {
		comments.add(new Comment(pos, "isPassed " + ai, ai));
		// This is the same implementation as "canBeType" - is that correct?
		types.add(new PosType(loc, ai));
	}

	@Override
	public void requireListMessage(InputPosition pos, String err) {
		errorConstraints.add(new ErrorConstraint(x -> TypeHelpers.isListMessage(pos, x), pos, err));
	}

	@Override
	public void requirePrimitive(InputPosition pos, String err) {
		errorConstraints.add(new ErrorConstraint(x -> TypeHelpers.isPrimitive(x), pos, err));
	}

	@Override
	public void requirePrimitiveOfString(InputPosition pos, String notPrimMsg, String notStringMsg) {
		errorConstraints.add(
			new ErrorConstraint(x -> TypeHelpers.isPrimitiveString(x), pos, notPrimMsg)
				.chain(new ErrorConstraint(x -> TypeHelpers.isPrimitiveString(x), pos, notStringMsg)));
	}

	@Override
	public void requireNonPrimitive(InputPosition pos, String err) {
		errorConstraints.add(new ErrorConstraint(x -> !TypeHelpers.isPrimitive(x), pos, err));
	}

	@Override
	public void callOnResolved(CallOnResolution handler) {
		onResolved.add(handler);
	}

	public String asTCS() {
		return "TCS:" + id + (isRedirected()?"=>"+redirectedTo().id():"") + "{" + (pos == null? "NULL":pos.inFile()) + ":" + (motive != null ? ":" + motive : "") + "}";
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
		if (usedOrReturned > 0) {
			ret.append(" +" + usedOrReturned);
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
			for (Type i : pi.polys()) {
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
