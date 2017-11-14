package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;
import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
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
		for (RWStructField f : sd.fields) {
			if (f.name.equals("id")) // don't try to require the "id" field
				continue;
			fs.add(tc.convertType(f.type));
		}
		TypeFunc ti = new TypeFunc(sd.location(), fs, sty);
		tc.gk(sd.uniqueName(), ti);
		tc.ctors.put(sd.name(), tc.asType(ti));
	}

	@Override
	public void visitObjectDefn(RWObjectDefn od) {
		System.out.println("Pass2 on an object definition");
	}

	public void visitContractDecl(RWContractDecl cd) {
		// nothing to do here
	}

	@Override
	public void visitCardGrouping(CardGrouping c) {
		String name = c.getName().uniqueName();
		// The elements of the card struct can appear directly as CardMembers
		// push their types into the knowledge
		for (RWStructField f : c.struct.fields) {
			// TODO: right now, I feel that renaming this is really a rewriter responsibility, but I'm not clear on the consequences
			TypeInfo ct = tc.convertType(f.type);
			tc.gk(name+"."+f.name, ct);
		}
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
		tc.export.put(hi.handlerName.uniqueName(), hi);
		List<TypeInfo> fs = new ArrayList<>();
		for (HandlerLambda f : hi.boundVars)
			if (f.scopedFrom == null)
				fs.add(tc.convertType(f.type));
		TypeFunc tf = new TypeFunc(hi.location(), fs, new NamedType(hi.location(), hi.handlerName));
		tc.gk(hi.handlerName.uniqueName(), tf);
		tc.ctors.put(hi.handlerName.uniqueName(), tc.asType(tf));
	}
}
