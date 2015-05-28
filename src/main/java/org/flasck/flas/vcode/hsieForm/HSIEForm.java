package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.zinutils.exceptions.UtilException;

public class HSIEForm extends HSIEBlock {
	public final String fnName;
	public final int nformal;
	public final List<Var> vars = new ArrayList<Var>();

	// This should really be objects, not names of objects
	public final Set<String> externals = new TreeSet<String>();
	private final Map<Var, HSIEBlock> closures = new HashMap<Var, HSIEBlock>();

	// This constructor is the one for real code
	public HSIEForm(String name, int nformal) {
		this.fnName = name;
		this.nformal = nformal;
	}

	// This constructor is for testing
	public HSIEForm(String name, int nformal, int nbound, List<String> dependsOn) {
		fnName = name;
		this.nformal = nformal;
		for (int i=0;i<nformal;i++)
			vars.add(new Var(i));
		for (int i=0;i<nbound;i++)
			vars.add(new Var(nformal + i));
		this.externals.addAll(dependsOn);
	}

	public Var var(int v) {
		return vars.get(v);
	}

	public HSIEBlock closure(Var var) {
		ClosureCmd ret = new ClosureCmd(var);
		closures.put(var, ret);
		return ret;
	}

	public HSIEBlock getClosure(Var v) {
		return closures.get(v);
	}

	public void dump() {
		System.out.println("#Args: " + nformal + " #bound: " + (vars.size()-nformal) + " externals: " + externals);
		dump(0);
		for (HSIEBlock c : closures.values())
			c.dumpOne(0);
	}

	public void dependsOn(String text) {
		if (!text.equals(this.fnName))
			externals.add(text);
	}
	
	// So, basically an HSIE definition consists of
	// Fn "name" [formal-args] [bound-vars] [external-vars]
	//   HEAD var
	//   SWITCH var Type/Constructor|Type|Type/Constructor
	//     BIND new-var var "field"
	//     IF boolean-expr
	//       EVAL En
	//   Er
	// If there is no general case, then add "E?" to indicate an error in switching

	// There is no notion of "Else", you just drop down to the next statement at a not-indented level and pick up from there.
	
	// Each of the Expressions En is modified to be just a simple apply-tree

}
