package org.flasck.flas.lifting;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIPatternOptions;

public class MappingStore implements MappingCollector, NestedVarReader {
	public class PO implements Comparable<PO> {
		VarName name;
		Pattern p;
		HSIPatternOptions opts;
		UnresolvedVar var;
		
		public PO(VarPattern p, FunctionIntro fi) {
			this(fi, p, p.name());
			this.opts.addVar(p.name(), fi);
			this.var.bind(p);
		}

		public PO(TypedPattern p, FunctionIntro fi) {
			this(fi, p, p.name());
			this.opts.addTyped(p.type, p.name(), fi);
			this.var.bind(p);
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
	
	@Override
	public void recordNestedVar(FunctionIntro fi, VarPattern vp) {
		patterns.add(new PO(vp, fi));
	}

	@Override
	public void recordNestedVar(FunctionIntro fi, TypedPattern tp) {
		patterns.add(new PO(tp, fi));
	}

	@Override
	public void recordDependency(FunctionDefinition from, FunctionDefinition to) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<HSIOptions> all() {
		return patterns.stream().map(po -> po.opts).collect(Collectors.toList());
	}

	@Override
	public List<UnresolvedVar> vars() {
		return patterns.stream().map(po -> po.var).collect(Collectors.toList());
	}

	public boolean isInteresting() {
		return !patterns.isEmpty();
	}
}
