package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.droidgen.DroidPushArgument;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.slf4j.Logger;
import org.zinutils.bytecode.IExpr;
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
	public boolean justScoping() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	// this is really a visit pattern and we should combine cxt & dpa in a visit(X) method ...
	@Override
	public IExpr arguments(GenerationContext cxt, DroidPushArgument dpa, int from) {
		// note that when we come to abolish nestedCommands(), we should just move that logic here ...
		cxt.beginClosure();
		for (int i=from;i<nestedCommands().size();i++) {
			PushReturn c = (PushReturn) nestedCommands().get(i);
			cxt.closureArg(c.visit(dpa));
		}
		return cxt.endClosure();
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
		for (HSIEBlock c : nestedCommands())
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
