package org.flasck.flas.lifting;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.NotImplementedException;

// This is a record for a single function
public class MappingStore implements MappingCollector, NestedVarReader {
	public static final Logger logger = LoggerFactory.getLogger("Lifter");
	public class PO implements Comparable<PO> {
		VarName name;
		Pattern p;
		HSIPatternOptions opts;
		UnresolvedVar var;
		
		public PO(VarPattern p, FunctionIntro fi, ObjectActionHandler meth) {
			this(fi, meth, p, p.name());
			this.opts.addVar(p, fi);
			this.var.bind(p);
		}

		public PO(TypedPattern p, FunctionIntro fi, ObjectActionHandler meth) {
			this(fi, meth, p, p.name());
			this.opts.addTyped(p, fi);
			this.var.bind(p);
		}

		// for the merging case
		public PO(PO o, FunctionIntro fi, ObjectActionHandler meth) {
			this(fi, meth, new VarPattern(o.p.location(), o.name), o.name);
			if (o.p instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) o.p;
				this.opts.addVarWithType(tp.type, tp.var, fi);
				this.var.bind(tp);
			} else {
				this.opts.addVar((VarPattern) o.p, fi);
				this.var.bind((VarPattern)o.p);
			}
		}

		private PO(FunctionIntro fi, ObjectActionHandler meth, Pattern p, VarName name) {
			this.p = p;
			this.name = name;
			this.opts = new HSIPatternOptions();
			if (fi != null) {
			this.opts.includes(fi);
			this.var = new UnresolvedVar(fi.location, this.name.var);
			} else {
				this.var = new UnresolvedVar(meth.location(), this.name.var);
			}
		}

		@Override
		public int compareTo(PO o) {
			return name().compareTo(o.name());
		}

		private String name() {
			return name.uniqueName();
		}
	}

	private final FunctionName name; 
	private TreeSet<PO> patterns = new TreeSet<>();
	private Set<LogicHolder> deps = new HashSet<>();
	
	public MappingStore(FunctionName name) {
		logger.info("Checking dependencies for " + name.uniqueName());
		this.name = name;
	}
	
	@Override
	public void recordNestedVar(FunctionIntro fi, ObjectActionHandler meth, VarPattern vp) {
		patterns.add(new PO(vp, fi, meth));
	}

	@Override
	public void recordNestedVar(FunctionIntro fi, ObjectActionHandler meth, TypedPattern tp) {
		patterns.add(new PO(tp, fi, meth));
	}

	@Override
	public void recordDependency(LogicHolder fn) {
		if (fn == null)
			throw new RuntimeException("Cannot depend on null function");
		logger.debug("  " + this + " depends on " + fn.name().uniqueName());
		deps.add(fn);
	}

	@Override
	public int size() {
		return patterns.size();
	}

	@Override
	public boolean containsReferencesNotIn(Set<LogicHolder> resolved) {
		Set<LogicHolder> ret = new HashSet<>(deps);
		ret.removeAll(resolved);
		return !ret.isEmpty();
	}

	@Override
	public Set<LogicHolder> references() {
		return deps;
	}
	
	@Override
	public boolean dependsOn(LogicHolder fn) {
		return deps.contains(fn);
	}

	@Override
	public boolean enhanceWith(LogicHolder sd, NestedVarReader nestedVars) {
		if (nestedVars == null)
			return false;
		
		boolean more = false;
		TreeSet<PO> ops = ((MappingStore)nestedVars).patterns;
		for (PO o : ops) {
			if (sd.isMyName(o.name.scope))
				continue;

			if (sd instanceof FunctionDefinition) {
				FunctionDefinition fn = (FunctionDefinition) sd;
				for (FunctionIntro fi : fn.intros())
					more |= patterns.add(new PO(o, fi, null));
			} else if (sd instanceof ObjectMethod) {
				more |= patterns.add(new PO(o, null, (ObjectMethod)sd));
			} else
				throw new NotImplementedException("cannot enhance a " + sd.getClass().getName() + ": " + sd.name());
		}
		return more;
	}

	@Override
	public Collection<HSIOptions> all() {
		return patterns.stream().map(po -> po.opts).collect(Collectors.toList());
	}

	@Override
	public List<UnresolvedVar> vars() {
		return patterns.stream().map(po -> po.var).collect(Collectors.toList());
	}

	@Override
	public List<Pattern> patterns() {
		return patterns.stream().map(po -> po.p).collect(Collectors.toList());
	}

	public boolean isInteresting() {
		return !patterns.isEmpty() || !deps.isEmpty();
	}
	
	@Override
	public String toString() {
		return name.uniqueName();
	}
}
