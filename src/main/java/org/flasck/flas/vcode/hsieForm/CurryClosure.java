package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.slf4j.Logger;
import org.zinutils.utils.Justification;

public class CurryClosure implements ClosureGenerator {

	private final HSIEBlock c;
	private final Var v;
	private final PushExternal pe;
	private final int arity;

	public CurryClosure(HSIEBlock c, int arity) {
		this.c = c;
		this.v = null;
		this.pe = null;
		this.arity = arity;
		if (c.nestedCommands().isEmpty())
			throw new RuntimeException("You suck");
	}

	public CurryClosure(Var v, PushExternal pe, int arity) {
		this.c = null;
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
	public boolean justScoping() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	public List<HSIEBlock> myNestedCommands() {
		List<HSIEBlock> ret = new ArrayList<>();
		HSIEBlock pc = pe;
		if (pe == null)
			pc = c.nestedCommands().get(0);
		ret.add(ClosureCmd.dopush(pc.location, new PackageVar(null, FunctionName.function(pc.location, new PackageName("FLEval"), "curry"), null), null));
		ret.add(pc);
		ret.add(ClosureCmd.dopush(pc.location, new NumericLiteral(pc.location, arity), null));
		if (c != null) {
			for (int i=1;i<c.nestedCommands().size();i++)
				ret.add(c.nestedCommands().get(i));
		}
		return ret;
	}

	@Override
	public Object arguments(ExprHandler h, int from) {
		// note that when we come to abolish nestedCommands(), we should just move that logic here ...
		h.beginClosure();
		for (int i=from;i<myNestedCommands().size();i++) {
			PushReturn c = (PushReturn) myNestedCommands().get(i);
			h.visit(c);
		}
		return h.endClosure();
	}

	public Object handleCurry(boolean needsCard, ExprHandler h) {
		PushExternal curriedFn = (PushExternal)myNestedCommands().get(1);
		ExternalRef f2 = curriedFn.fn;
		NameOfThing clz = f2.myName();
		ExprHandler h1;
		if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
			if (needsCard)
				h1 = h.curry(clz, ObjectNeeded.CARD, arity);
			else
				h1 = h.curry(clz, ObjectNeeded.THIS, arity);
		} else {
			h1 = h.curry(clz, ObjectNeeded.NONE, arity);
		}
		return arguments(h1, 3);
	}


	@Override
	public List<VarInSource> dependencies() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void dumpOne(Logger logTo, int ind) {
		if (c == null) {
			logTo.info(asString("CLOSURE " + v));
		} else
			c.dumpOne(logTo, ind);
	}

	public void dump(PrintWriter pw, int ind) {
		// When this goes away, change the formatting
		for (HSIEBlock c : myNestedCommands())
			c.dumpOne(pw, ind);
		pw.flush();
	}

	@Override
	public void dumpOne(PrintWriter pw, int ind) {
		if (c == null) {
			pw.println(asString("CLOSURE " + v));
		} else
			pw.println(c.asString(ind));
		dump(pw, ind+2);
		pw.flush();
	}

	protected String asString(String s) {
		return Justification.LEFT.format(s, 60) + " #" + pe.location;
	}

}
