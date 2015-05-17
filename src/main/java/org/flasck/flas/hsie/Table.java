package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;

public class Table {
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

}
