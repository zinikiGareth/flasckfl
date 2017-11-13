package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.ScopedVar;

public interface PushVisitor<T> {
	public void visit(PushVar pv, OutputHandler<T> handler);
	public void visit(PushInt pi, OutputHandler<T> handler);
	public void visit(PushDouble pd, OutputHandler<T> handler);
	public void visit(PushString ps, OutputHandler<T> handler);
	public void visit(PushBool pb, OutputHandler<T> handler);
	public void visit(PushTLV pt, OutputHandler<T> handler);
	public void visit(PushCSR pc, OutputHandler<T> handler);
	public void visit(PushFunc pf, OutputHandler<T> handler);
	public void visit(PushBuiltin pb, OutputHandler<T> handler);

	public void visitExternal(CardMember cm, OutputHandler<T> handler);
	public void visitExternal(CardFunction cf, OutputHandler<T> handler);
	public void visitExternal(HandlerLambda hl, OutputHandler<T> handler);
	public void visitExternal(ScopedVar sv, OutputHandler<T> handler);
	public void visitExternal(ObjectReference or, OutputHandler<T> handler);
	public void visitExternal(PackageVar pv, OutputHandler<T> handler);
}
