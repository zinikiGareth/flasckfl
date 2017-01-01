package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;

public class Pass2Visitor implements RepoVisitor {
	private final TypeChecker2 tc;

	public Pass2Visitor(TypeChecker2 tc) {
		this.tc = tc;
	}

	@Override
	public void visitStructDefn(RWStructDefn sd) {
		TypeInfo sty = tc.structTypes.get(sd.uniqueName());
		List<TypeInfo> fs = new ArrayList<>();
		for (RWStructField f : sd.fields)
			fs.add(tc.convertType(f.type));
		TypeFunc ti = new TypeFunc(sd.location(), fs, sty);
		tc.gk(sd.uniqueName(), ti);
		tc.ctors.put(sd.name(), tc.asType(ti));
	}

	public void visitContractDecl(RWContractDecl cd) {
	}

	@Override
	public void visitCardGrouping(CardGrouping c) {
		String name = c.name().uniqueName();
		// The elements of the card struct can appear directly as CardMembers
		// push their types into the knowledge
		for (RWStructField f : c.struct.fields) {
			// TODO: right now, I feel that renaming this is really a rewriter responsibility, but I'm not clear on the consequences
			TypeInfo ct = tc.convertType(f.type);
			tc.gk(name+"."+f.name, ct);
		}
	}
}
