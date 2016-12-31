package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewriter.RepoVisitor;
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

}
