package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.types.Type;

public class Pass1Visitor implements RepoVisitor {
	private final TypeChecker2 tc;

	public Pass1Visitor(TypeChecker2 tc) {
		this.tc = tc;
	}

	@Override
	public void visitStructDefn(RWStructDefn sd) {
		tc.structs.put(sd.uniqueName(), sd);
		List<TypeInfo> polys = new ArrayList<>();
		if (sd.hasPolys()) {
			for (Type t : sd.polys())
				polys.add(tc.convertType(t));
		}
		tc.structTypes.put(sd.uniqueName(), new NamedType(sd.location(), sd.getTypeName(), polys));
	}

	@Override
	public void visitObjectDefn(RWObjectDefn od) {
		tc.objects.put(od.uniqueName(), od);
		List<TypeInfo> polys = new ArrayList<>();
		if (od.hasPolys()) {
			for (Type t : od.polys())
				polys.add(tc.convertType(t));
		}
		tc.structTypes.put(od.uniqueName(), new NamedType(od.location(), od.getTypeName(), polys));
	}

	public void visitContractDecl(RWContractDecl cd) {
		tc.gk(cd.name(), new NamedType(cd.location(), cd.getTypeName()));
	}

	@Override
	public void visitCardGrouping(CardGrouping c) {
		String name = c.getName().uniqueName();
		RWStructDefn str = c.struct;
		tc.structs.put(name, str);
		tc.gk(name, new NamedType(str.location(), str.getTypeName()));
	}

	@Override
	public void visitContractImpl(RWContractImplements ci) {
		// nothing to do here
	}

	@Override
	public void visitServiceImpl(RWContractService cs) {
		// nothing to do here
	}

	@Override
	public void visitHandlerImpl(RWHandlerImplements hi) {
		// nothing to do here
	}
}
