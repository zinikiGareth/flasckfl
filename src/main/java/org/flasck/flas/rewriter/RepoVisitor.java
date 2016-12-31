package org.flasck.flas.rewriter;

import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.NewMethodDefiner;

public interface RepoVisitor {

	void visitStructDefn(RWStructDefn sd);

	void visitContractDecl(RWContractDecl cd);

}
