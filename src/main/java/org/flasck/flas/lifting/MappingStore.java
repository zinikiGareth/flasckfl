package org.flasck.flas.lifting;

import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIPatternOptions;

public class MappingStore implements MappingCollector, NestedVarReader {
	public class PO implements Comparable<PO> {
		VarName name;
		Pattern p;
		HSIPatternOptions opts;
		
		public PO(VarPattern p, FunctionIntro fi) {
			this.name = p.name();
			this.p = p;
			this.opts = new HSIPatternOptions();
			this.opts.includes(fi);
			this.opts.addVar(p.name(), fi);
		}

		public PO(TypedPattern p, FunctionIntro fi) {
			this.name = p.name();
			this.p = p;
			this.opts = new HSIPatternOptions();
			this.opts.includes(fi);
			this.opts.addTyped(p.type, p.name(), fi);
		}

		@Override
		public int compareTo(PO o) {
			return name().compareTo(o.name());
		}

		private String name() {
			if (p instanceof VarPattern)
				return ((VarPattern)p).name().uniqueName();
			else if (p instanceof TypedPattern)
				return ((TypedPattern)p).name().uniqueName();
			else
				return "";
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

}
