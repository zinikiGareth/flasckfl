package org.flasck.flas.hsie;

import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.flasck.flas.vcode.hsieForm.PushBuiltin;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushDouble;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.PushVisitor;

public final class PushArgumentTraverser<T> implements PushVisitor<T> {
	private final HSIEForm form;
	private final GenerationContext<T> cxt;

	public PushArgumentTraverser(HSIEForm form, GenerationContext<T> cxt) {
		this.form = form;
		this.cxt = cxt;
	}

	@Override
	public void visitExternal(CardMember cm, OutputHandler<T> handler) {
		cxt.generateCardMember().push(cm, form, handler);
	}

	@Override
	public void visitExternal(CardFunction cf, OutputHandler<T> handler) {
		cxt.generateCardFunction().push(cf, handler);
	}

	@Override
	public void visitExternal(HandlerLambda hl, OutputHandler<T> handler) {
		cxt.generateHandlerLambda().push(hl, form, handler);
	}

	@Override
	public void visitExternal(ScopedVar sv, OutputHandler<T> handler) {
		cxt.generateScopedVar().push(sv, form, handler);
	}

	@Override
	public void visitExternal(ObjectReference or, OutputHandler<T> handler) {
		cxt.generateObjectReference().push(or, handler);
	}

	@Override
	public void visitExternal(PackageVar pv, OutputHandler<T> handler) {
		Object defn = pv.defn;
		if (defn instanceof RWStructDefn)
			cxt.generateStructDefn().push((RWStructDefn)defn, handler);
		else if (defn instanceof RWFunctionDefinition)
			cxt.generateFunctionDefn().push((RWFunctionDefinition)defn, handler);
		else
			throw new RuntimeException("Cannot push external with " + defn.getClass());
	}

	@Override
	public void visit(PushBuiltin pb, OutputHandler<T> handler) {
		cxt.generateBuiltinOp().push(pb, handler);
	}

	@Override
	public void visit(PushVar pv, OutputHandler<T> handler) {
		cxt.generateVar().push(pv, handler);
	}

	@Override
	public void visit(PushInt pi, OutputHandler<T> handler) {
		cxt.generateInt().push(pi, handler);
	}

	@Override
	public void visit(PushDouble pd, OutputHandler<T> handler) {
		cxt.generateDouble().push(pd, handler);
	}

	@Override
	public void visit(PushString ps, OutputHandler<T> handler) {
		cxt.generateString().push(ps, handler);
	}

	@Override
	public void visit(PushBool pb, OutputHandler<T> handler) {
		cxt.generateBool().push(pb, handler);
	}

	@Override
	public void visit(PushTLV pt, OutputHandler<T> handler) {
		cxt.generateTLV().push(pt, handler);
	}

	@Override
	public void visit(PushCSR pc, OutputHandler<T> handler) {
		cxt.generateCSR().push(pc, handler);
	}

	@Override
	public void visit(PushFunc pf, OutputHandler<T> handler) {
		cxt.generateFunc().push(pf, handler);
	}
}