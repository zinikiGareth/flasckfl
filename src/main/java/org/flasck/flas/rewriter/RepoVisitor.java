package org.flasck.flas.rewriter;

import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWStructDefn;

public interface RepoVisitor {

	void visitStructDefn(RWStructDefn sd);

	void visitContractDecl(RWContractDecl cd);

	void visitCardGrouping(CardGrouping c);

	void visitContractImpl(RWContractImplements ci);

	void visitServiceImpl(RWContractService cs);
}
