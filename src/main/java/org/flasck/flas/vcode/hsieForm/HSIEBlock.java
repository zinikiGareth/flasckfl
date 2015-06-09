package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.StringLiteral;
import org.zinutils.exceptions.UtilException;

public class HSIEBlock {
	private final List<HSIEBlock> commands = new ArrayList<HSIEBlock>();

	public void head(Var v) {
		commands.add(new Head(v));
	}

	public HSIEBlock switchCmd(Var v, String ctor) {
		Switch ret = new Switch(v, ctor);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(Var v, int value) {
		IFCmd ret = new IFCmd(v, value);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock bindCmd(Var bind, Var from, String field) {
		BindCmd ret = new BindCmd(bind, from, field);
		commands.add(ret);
		return ret;
	}
	
	public HSIEBlock push(Object o) {
		return pushAt(commands.size(), o);
	}

	public HSIEBlock pushAt(int pos, Object o) {
		PushCmd ret;
		if (o instanceof Var)
			ret = new PushCmd((Var)o);
		else if (o instanceof Integer)
			ret = new PushCmd((Integer)o);
		else if (o instanceof ExternalRef)
			ret = new PushCmd((ExternalRef)o);
		else if (o instanceof StringLiteral)
			ret = new PushCmd((StringLiteral)o);
		else if (o == null)
			throw new UtilException("Cannot push null");
		else
			throw new UtilException("Invalid object to push " + o.getClass() + ": " + o);
		commands.add(pos, ret);
		return ret;
	}

	public HSIEBlock doReturn(Object o, List<Var> deps) {
		ReturnCmd ret;
		if (o instanceof Var)
			ret = new ReturnCmd((Var)o, deps);
		else if (o instanceof Integer)
			ret = new ReturnCmd((Integer)o);
		else if (o instanceof StringLiteral)
			ret = new ReturnCmd((StringLiteral)o);
		else if (o instanceof String)
			// TODO: check it's a defined name
			ret = new ReturnCmd((String)o);
		else
			throw new UtilException("Invalid object to return");
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