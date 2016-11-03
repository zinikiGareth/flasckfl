package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.zinutils.exceptions.UtilException;

public class Table implements Iterable<Option> {
	private final List<Option> options = new ArrayList<Option>();

	public Option createOption(InputPosition loc, Var var) {
		Option ret = new Option(loc, var);
		options.add(ret);
		return ret;
	}

	public void remove(Option o) {
		if (!options.contains(o))
			throw new UtilException("Option " + o + " is not in my list");
		options.remove(o);
	}

	public void dump() {
		for (Option o : options) {
			o.dump();
		}
	}

	public void dump(Logger logger) {
		for (Option o : options) {
			o.dump(logger);
		}
	}

	@Override
	public Iterator<Option> iterator() {
		return options.iterator();
	}
}
