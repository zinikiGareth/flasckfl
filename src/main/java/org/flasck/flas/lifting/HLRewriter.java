package org.flasck.flas.lifting;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.zinutils.exceptions.NotImplementedException;

public class HLRewriter extends LeafAdapter {
	private final String name;
	private final HandlerLambda hl;

	public HLRewriter(Pattern p, HandlerLambda hl) {
		if (p instanceof VarPattern)
			this.name = ((VarPattern)p).name().uniqueName();
		else if (p instanceof TypedPattern)
			this.name = ((TypedPattern)p).name().uniqueName();
		else
			throw new NotImplementedException();
		this.hl = hl;
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (var.defn() instanceof VarPattern) {
			String n = ((VarPattern)var.defn()).name().uniqueName();
			if (n.equals(this.name))
				var.bind(hl);
		} else if (var.defn() instanceof TypedPattern) {
			String n = ((TypedPattern)var.defn()).name().uniqueName();
			if (n.equals(this.name))
				var.bind(hl);
		} else if (var.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) var.defn();
			if (fn.nestedVars() != null) {
				NestedVarReader nv = fn.nestedVars();
				int i = 0;
				for (Pattern p : nv.patterns()) {
					if (p instanceof VarPattern) {
						String n = ((VarPattern)p).name().uniqueName();
						if (n.equals(this.name))
							nv.bindLambda(i, hl);
					}
				}
			}
		}
	}
}
