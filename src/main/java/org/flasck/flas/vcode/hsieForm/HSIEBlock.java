package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Justification;

public class HSIEBlock {
	public final InputPosition location;
	// The downcast operator has the ability to set the output type of this block "by fiat"
	public Type downcastType;
	private final List<HSIEBlock> commands = new ArrayList<HSIEBlock>();
	protected static final Logger logger = LoggerFactory.getLogger("HSIE");

	public HSIEBlock(InputPosition loc) {
		if (loc == null)
			throw new UtilException("Location cannot be null");
		this.location = loc;
	}
	
	public void head(InputPosition loc, Var v) {
		commands.add(new Head(loc, v));
	}

	public HSIEBlock switchCmd(InputPosition loc, Var v, String ctor) {
		Switch ret = new Switch(loc, v, ctor);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(InputPosition loc, VarInSource var) {
		IFCmd ret = new IFCmd(loc, var);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(InputPosition loc, VarInSource v, Object value) {
		IFCmd ret = new IFCmd(loc, v, value);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(InputPosition loc, VarInSource v, boolean value) {
		IFCmd ret = new IFCmd(loc, v, value);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock bindCmd(InputPosition loc, Var bind, Var from, String field) {
		BindCmd ret = new BindCmd(loc, bind, from, field);
		commands.add(ret);
		return ret;
	}
	
	public void removeAt(int pos) {
		commands.remove(pos);
	}

	public PushReturn push(InputPosition loc, Object o, List<VarInSource> list) {
		PushReturn ret = dopush(loc, o, list);
		commands.add(ret);
		return ret;
	}

	public static PushReturn dopush(InputPosition loc, Object o, List<VarInSource> list) {
		PushReturn ret;
		if (o == null)
			throw new UtilException("Cannot push null");
		else if (!(o instanceof Pushable))
			throw new UtilException("Invalid object to push " + o.getClass() + ": " + o);

		ret = ((Pushable)o).hsie(loc, list);
		return ret;
	}

	public PushReturn pushAt(InputPosition loc, int pos, Object o) {
		PushReturn ret = dopush(loc, o, null);
		commands.add(pos, ret);
		return ret;
	}

	public void caseError() {
		ErrorCmd ret = new ErrorCmd(location);
		commands.add(ret);
	}

	public List<HSIEBlock> nestedCommands() {
		return commands;
	}

	public <T> T visit(HSIEVisitor<T> v) {
		for (HSIEBlock n : commands) {
			if (n instanceof Head)
				v.visit((Head)n);
			else if (n instanceof Switch)
				v.visit((Switch)n);
			else if (n instanceof BindCmd)
				v.visit((BindCmd)n);
			else if (n instanceof IFCmd)
				v.visit((IFCmd)n);
			else if (n instanceof PushReturn)
				v.visit((PushReturn)n);
			else if (n instanceof ErrorCmd)
				v.visit((ErrorCmd)n);
			else
				throw new UtilException("Cannot handle " + n.getClass());
		}
		return v.done();
	}

	protected void dump(Logger logTo, int ind) {
		for (HSIEBlock c : commands)
			c.dumpOne(logTo, ind);
	}

	public void dumpOne(Logger logTo, int ind) {
		if (logTo == null)
			logTo = logger ;
		logTo.info(asString(ind));
		dump(logTo, ind+2);
	}

	public void dump(PrintWriter pw, int ind) {
		for (HSIEBlock c : commands)
			c.dumpOne(pw, ind);
		pw.flush();
	}

	public void dumpOne(PrintWriter pw, int ind) {
		pw.println(asString(ind));
		dump(pw, ind+2);
		pw.flush();
	}
	
	protected String asString(int ind) {
		return Justification.LEFT.format(Justification.LEFT.format("", ind) + this, 60) + " #" + location;
	}
}