package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;
import org.zinutils.exceptions.UtilException;

public class HSIEBlock {
	private final List<HSIEBlock> commands = new ArrayList<HSIEBlock>();

	public void head(HSIEForm form, int v) {
		commands.add(new Head(form.var(v)));
	}

	public HSIEBlock switchCmd(HSIEForm form, int v, String ctor) {
		Switch ret = new Switch(form.var(v), ctor);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock ifCmd(HSIEForm form, int v, int value) {
		IFCmd ret = new IFCmd(form.var(v), value);
		commands.add(ret);
		return ret;
	}
	
	public HSIEBlock closure(Var var) {
		ClosureCmd ret = new ClosureCmd(var);
		commands.add(ret);
		return ret;
	}

	public HSIEBlock push(Object o) {
		PushCmd ret;
		if (o instanceof Var)
			ret = new PushCmd((Var)o);
		else if (o instanceof Integer)
			ret = new PushCmd((Integer)o);
		else if (o instanceof String)
			// TODO: check it's a defined name
			ret = new PushCmd((String)o);
		else
			throw new UtilException("Invalid object to push");
		commands.add(ret);
		return ret;
	}

	public HSIEBlock doReturn(Object o) {
		ReturnCmd ret;
		if (o instanceof Var)
			ret = new ReturnCmd((Var)o);
		else if (o instanceof Integer)
			ret = new ReturnCmd((Integer)o);
		else if (o instanceof String)
			// TODO: check it's a defined name
			ret = new ReturnCmd((String)o);
		else
			throw new UtilException("Invalid object to return");
		commands.add(ret);
		return ret;
	}

	public List<HSIEBlock> nestedCommands() {
		return commands;
	}

	protected void dump(int ind) {
		for (HSIEBlock c : commands) {
			for (int i=0;i<ind;i++)
				System.out.print(' ');
			System.out.println(c);
			c.dump(ind+2);
		}
	}
}