package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.slf4j.Logger;
import org.zinutils.utils.Justification;

public class CurryClosure implements ClosureGenerator {
	private final HSIEBlock c;
	private final Var v;
	private final PushExternal pe;
	private final int arity;
	private final boolean scoping;

	public CurryClosure(ClosureCmd c, int arity, boolean scoping) {
		this.c = c;
		this.scoping = scoping;
		this.v = c.var;
		this.pe = (PushExternal) c.nestedCommands().get(0);
		this.arity = arity;
	}

	public CurryClosure(Var v, PushExternal pe, int arity) {
		this.c = null;
		this.scoping = false;
		this.v = v;
		this.pe = pe;
		this.arity = arity;
	}

	// remembering that I ultimately want to do away with this and make it inverted logic ...
	@Override
	public List<HSIEBlock> nestedCommands() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public List<VarInSource> dependencies() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	// This is something that appears to be a factor, but doesn't seem to affect any current golden tests ...
	@Override
	public boolean justScoping() {
		return scoping;
	}

	@Override
	public <T> void arguments(HSIEForm form, ClosureHandler<T> h, int from, OutputHandler<T> handler) {
		if (from != 3) throw new RuntimeException();
		h.beginClosure();
		if (c != null) {
			for (int i=1;i<c.nestedCommands().size();i++) {
				PushReturn pc = (PushReturn) c.nestedCommands().get(i);
				h.visit(form, pc);
			}
		}
		h.endClosure(handler);
	}

	public <T> void handleCurry(HSIEForm form, boolean needsCard, ClosureHandler<T> h, OutputHandler<T> handler) {
		PushExternal curriedFn = pe;
		if (pe == null)
			curriedFn = (PushExternal)c.nestedCommands().get(0);
		ExternalRef f2 = curriedFn.fn;
		NameOfThing clz = f2.myName();
		ClosureHandler<T> h1;
		if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
			if (needsCard)
				h1 = h.curry(clz, ObjectNeeded.CARD, arity);
			else
				h1 = h.curry(clz, ObjectNeeded.THIS, arity);
		} else {
			h1 = h.curry(clz, ObjectNeeded.NONE, arity);
		}
		arguments(form, h1, 3, handler);
	}

	@Override
	public void dumpOne(Logger logTo, int ind) {
		logTo.info(toString());
		if (c == null) {
			pe.dumpOne(logTo, ind);
		} else {
			for (HSIEBlock b : c.nestedCommands())
				b.dumpOne(logTo, ind);
		}
	}

	public void dump(PrintWriter pw, int ind) {
		if (c == null)
			pe.dumpOne(pw, ind);
		else {
			for (HSIEBlock b : c.nestedCommands())
				b.dumpOne(pw, ind);
			pw.flush();
		}
	}

	@Override
	public void dumpOne(PrintWriter pw, int ind) {
		pw.println(toString());
		dump(pw, ind+2);
		pw.flush();
	}

	@Override
	public String toString() {
		return asString("CURRY " + v + " " + arity + (scoping?" !":""));
	}
	
	protected String asString(String s) {
		return Justification.LEFT.format(s, 60) + " #" + pe.location;
	}

}
