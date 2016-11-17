package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
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

	public PushReturn push(InputPosition loc, Object o) {
		return pushAt(loc, commands.size(), o);
	}

	public PushReturn pushAt(InputPosition loc, int pos, Object o) {
		PushReturn ret;
		if (o instanceof VarInSource)
			ret = new PushVar(loc, (VarInSource)o);
		else if (o instanceof Integer)
			ret = new PushInt(loc, (Integer)o);
		else if (o instanceof ExternalRef)
			ret = new PushExternal(loc, (ExternalRef)o);
		else if (o instanceof StringLiteral)
			ret = new PushString(loc, (StringLiteral)o);
		else if (o instanceof TemplateListVar)
			ret = new PushTLV(loc, (TemplateListVar)o);
		else if (o instanceof FunctionLiteral)
			ret = new PushFunc(loc, (FunctionLiteral)o);
		else if (o instanceof CardStateRef)
			ret = new PushCSR(loc, (CardStateRef)o);
		else if (o == null)
			throw new UtilException("Cannot push null");
		else
			throw new UtilException("Invalid object to push " + o.getClass() + ": " + o);
		commands.add(pos, ret);
		return ret;
	}

	public HSIEBlock doReturn(InputPosition loc, Object o, List<VarInSource> list) {
		PushReturn ret;
		if (o == null)
			throw new UtilException("Attempt to return null");
		if (o instanceof VarInSource)
			ret = new PushVar(loc, (VarInSource)o, list);
		else if (o instanceof Integer)
			ret = new PushInt(loc, (Integer)o);
		else if (o instanceof StringLiteral)
			ret = new PushString(loc, (StringLiteral)o);
		else if (o instanceof ExternalRef)
			ret = new PushExternal(loc, (ExternalRef)o);
		else if (o instanceof TemplateListVar)
			ret = new PushTLV(loc, (TemplateListVar)o);
		else
			throw new UtilException("Invalid object to return: " + o + " of type " + o.getClass());
		ret.asReturn();
		commands.add(ret);
		return ret;
	}

	public void caseError() {
		ErrorCmd ret = new ErrorCmd(location);
		commands.add(ret);
	}

	public List<HSIEBlock> nestedCommands() {
		return commands;
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
	}

	public void dumpOne(PrintWriter pw, int ind) {
		pw.println(asString(ind));
		dump(pw, ind+2);
	}
	
	protected String asString(int ind) {
		return Justification.LEFT.format(Justification.LEFT.format("", ind) + this, 60) + " #" + location;
	}
}