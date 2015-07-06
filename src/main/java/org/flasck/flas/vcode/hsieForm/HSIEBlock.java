package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.zinutils.exceptions.UtilException;

public class HSIEBlock {
	private final List<HSIEBlock> commands = new ArrayList<HSIEBlock>();

	public void head(Var v) {
		commands.add(new Head(v));
	}

	public HSIEBlock switchCmd(InputPosition loc, Var v, String ctor) {
		Switch ret = new Switch(loc, v, ctor);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(Var var) {
		IFCmd ret = new IFCmd(var);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(Var v, Object value) {
		IFCmd ret = new IFCmd(v, value);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(Var v, boolean value) {
		IFCmd ret = new IFCmd(v, value);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock bindCmd(Var bind, Var from, String field) {
		BindCmd ret = new BindCmd(bind, from, field);
		commands.add(ret);
		return ret;
	}
	
	public HSIEBlock push(InputPosition loc, Object o) {
		return pushAt(loc, commands.size(), o);
	}

	public HSIEBlock pushAt(InputPosition loc, int pos, Object o) {
		PushCmd ret;
		if (o instanceof Var)
			ret = new PushCmd(loc, (Var)o);
		else if (o instanceof Integer)
			ret = new PushCmd(loc, (Integer)o);
		else if (o instanceof ExternalRef)
			ret = new PushCmd(loc, (ExternalRef)o);
		else if (o instanceof StringLiteral)
			ret = new PushCmd(loc, (StringLiteral)o);
		else if (o instanceof TemplateListVar)
			ret = new PushCmd(loc, (TemplateListVar)o);
		else if (o instanceof FunctionLiteral)
			ret = new PushCmd(loc, (FunctionLiteral)o);
		else if (o == null)
			throw new UtilException("Cannot push null");
		else
			throw new UtilException("Invalid object to push " + o.getClass() + ": " + o);
		commands.add(pos, ret);
		return ret;
	}

	public HSIEBlock doReturn(InputPosition loc, Object o, List<Var> deps) {
		ReturnCmd ret;
		if (o instanceof Var)
			ret = new ReturnCmd(loc, (Var)o, deps);
		else if (o instanceof Integer)
			ret = new ReturnCmd(loc, (Integer)o);
		else if (o instanceof StringLiteral)
			ret = new ReturnCmd(loc, (StringLiteral)o);
		else if (o instanceof ExternalRef)
			ret = new ReturnCmd(loc, (ExternalRef)o);
		else if (o instanceof TemplateListVar)
			ret = new ReturnCmd(loc, (TemplateListVar)o);
		else
			throw new UtilException("Invalid object to return: " + o + " of type " + o.getClass());
		commands.add(ret);
		return ret;
	}

	public void caseError() {
		ErrorCmd ret = new ErrorCmd();
		commands.add(ret);
	}

	public List<HSIEBlock> nestedCommands() {
		return commands;
	}

	protected void dump(int ind) {
		for (HSIEBlock c : commands)
			c.dumpOne(ind);
	}

	public void dumpOne(int ind) {
		for (int i=0;i<ind;i++)
			System.out.print(' ');
		System.out.println(this);
		dump(ind+2);
	}
}