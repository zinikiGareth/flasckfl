package org.flasck.flas.rewriter;

import org.flasck.flas.rewrittenForm.RWStructDefn;

public interface RepoVisitor {

	void visitStructDefn(RWStructDefn sd);

}
