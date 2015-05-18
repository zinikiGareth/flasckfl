package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;

public class Table implements Iterable<Option> {
	private final List<Option> options = new ArrayList<Option>();

	public Option createOption(Var var) {
		Option ret = new Option(var);
		options.add(ret);
		return ret;
	}

	public void dump() {
		for (Option o : options) {
			o.dump();
		}
	}

	@Override
	public Iterator<Option> iterator() {
		return options.iterator();
	}

}
