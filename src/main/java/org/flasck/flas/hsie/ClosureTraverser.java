package org.flasck.flas.hsie;

import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.CurryClosure;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBuiltin;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.PushVisitor;
import org.zinutils.exceptions.UtilException;

public class ClosureTraverser<T> {
	private final HSIEForm form;
	private final ObjectNeeded myOn;
	private final PushVisitor<T> dpa;
	private final GenerationContext<T> cxt;
	
	public ClosureTraverser(HSIEForm form, GenerationContext<T> cxt) {
		this.form = form;
		this.cxt = cxt;
		dpa = new PushArgumentTraverser<T>(form, cxt);
		if (form.needsCardMember())
			myOn = ObjectNeeded.CARD;
		else if (form.isCardMethod())
			myOn = ObjectNeeded.THIS;
		else
			myOn = ObjectNeeded.NONE;
	}

	public void closure(ClosureGenerator closure, OutputHandler<T> handler) {
		if (closure instanceof CurryClosure)
			((CurryClosure)closure).handleCurry(form, form.needsCardMember(), cxt.getClosureHandler(), handler);
		else
			pushReturn((PushReturn) closure.nestedCommands().get(0), closure, handler);
	}

	public void pushReturn(PushReturn pr, ClosureGenerator closure, OutputHandler<T> handler) {
		if (pr instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)pr).fn;
			Object defn = fn;
			while (defn instanceof PackageVar)
				defn = ((PackageVar)defn).defn;
			if (defn instanceof PrimitiveType) {
				cxt.generatePrimitiveType().generate((PrimitiveType) defn, handler, closure);
			} else if (defn instanceof CardGrouping) {
				cxt.generateCardGrouping().generate((CardGrouping) defn, handler, closure);
			} else if (defn instanceof ObjectReference) {
				cxt.generateObjectReference().generate((ObjectReference) defn, myOn, handler, closure);
			} else if (defn instanceof CardFunction) {
				cxt.generateCardFunction().generate((CardFunction) defn, myOn, handler, closure);
			} else if (defn instanceof CardMember) {
				cxt.generateCardMember().generate((CardMember)defn, form, myOn, handler, closure);
			} else if (defn instanceof RWFunctionDefinition) {
				cxt.generateFunctionDefn().generate((RWFunctionDefinition) defn, handler, closure);
			} else if (defn instanceof RWObjectDefn) {
				cxt.generateObjectDefn().generate((RWObjectDefn) defn, handler, closure);
			} else if (defn instanceof RWStructDefn) {
				cxt.generateStructDefn().generate((RWStructDefn) defn, handler, closure);
			} else if (defn instanceof HandlerLambda) {
				cxt.generateHandlerLambda().generate((HandlerLambda) defn, handler, closure);
			} else if (defn instanceof ScopedVar) {
				cxt.generateScopedVar().generate((ScopedVar) defn, handler, closure);
			} else
				throw new UtilException("Didn't do anything with " + defn + " " + (defn != null ? defn.getClass() : ""));
		} else if (pr instanceof PushVar) {
			cxt.generateVar().generate((PushVar)pr, handler, closure);
		} else if (pr instanceof PushInt) {
			cxt.generateInt().generate((PushInt)pr, handler, closure);
		} else if (pr instanceof PushString) {
			cxt.generateString().generate((PushString)pr, handler, closure);
		} else if (pr instanceof PushTLV) {
			cxt.generateTLV().generate((PushTLV) pr, handler, closure);
		} else if (pr instanceof PushBuiltin) {
			cxt.generateBuiltinOp().generate((PushBuiltin) pr, dpa, handler, closure);
		} else
			throw new UtilException("Can't handle " + pr);
	}
}
