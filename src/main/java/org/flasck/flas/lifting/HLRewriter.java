package org.flasck.flas.lifting;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;

public class HLRewriter extends LeafAdapter {
	private final Pattern p;
	private final HandlerLambda hl;

	public HLRewriter(Pattern p, HandlerLambda hl) {
		this.p = p;
		this.hl = hl;
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (var.defn() == p)
			var.bind(hl);
	}
}
