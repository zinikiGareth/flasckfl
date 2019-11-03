package org.flasck.flas.lifting;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.zinutils.exceptions.NotImplementedException;

// This is a record per-function
public class MappingStore implements MappingCollector, NestedVarReader {
	public class PO implements Comparable<PO> {
		VarName name;
		Pattern p;
		HSIPatternOptions opts;
		UnresolvedVar var;
		
		public PO(VarPattern p, FunctionIntro fi) {
			this(fi, p, p.name());
			this.opts.addVar(p, fi);
			this.var.bind(p);
		}

		public PO(TypedPattern p, FunctionIntro fi) {
			this(fi, p, p.name());
			this.opts.addTyped(p, fi);
			this.var.bind(p);
		}

		// for the merging case
		public PO(PO o, FunctionIntro fi) {
			this(fi, new VarPattern(o.p.location(), o.name), o.name);
			if (o.p instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) o.p;
				this.opts.addVarWithType(tp.type, tp.var, fi);
				this.var.bind(tp);
			} else {
				this.opts.addVar((VarPattern) o.p, fi);
				this.var.bind((VarPattern)o.p);
			}
		}

		private PO(FunctionIntro fi, Pattern p, VarName name) {
			this.p = p;
			this.name = name;
			this.opts = new HSIPatternOptions();
			this.opts.includes(fi);
			this.var = new UnresolvedVar(fi.location, this.name.var);
		}

		@Override
		public int compareTo(PO o) {
			return name().compareTo(o.name());
		}

		private String name() {
			return name.uniqueName();
		}
	}

	private TreeSet<PO> patterns = new TreeSet<>();
	private Set<StandaloneDefn> deps = new HashSet<>(); 
	
	@Override
	public void recordNestedVar(FunctionIntro fi, VarPattern vp) {
		patterns.add(new PO(vp, fi));
	}

	@Override
	public void recordNestedVar(FunctionIntro fi, TypedPattern tp) {
		patterns.add(new PO(tp, fi));
	}

	@Override
	public void recordDependency(FunctionDefinition dependsOn) {
		deps.add(dependsOn);
	}

	@Override
	public void recordDependency(StandaloneMethod dependsOn) {
		deps.add(dependsOn);
	}

	@Override
	public int size() {
		return patterns.size();
	}

	@Override
	public boolean containsReferencesNotIn(Set<StandaloneDefn> resolved) {
		Set<StandaloneDefn> ret = new HashSet<>(deps);
		ret.removeAll(resolved);
		return !ret.isEmpty();
	}

	@Override
	public Set<StandaloneDefn> references() {
		return deps;
	}
	
	@Override
	public boolean dependsOn(StandaloneDefn fn) {
		return deps.contains(fn);
	}

	@Override
	public void enhanceWith(StandaloneDefn sd, NestedVarReader nestedVars) {
		if (nestedVars == null)
			return;
		
		TreeSet<PO> ops = ((MappingStore)nestedVars).patterns;
		for (PO o : ops) {
			if (o.name.scope == sd.name())
				continue;

			if (sd instanceof FunctionDefinition) {
				FunctionDefinition fn = (FunctionDefinition) sd;
				for (FunctionIntro fi : fn.intros())
					patterns.add(new PO(o, fi));
			} else
				throw new NotImplementedException();
		}
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
}
