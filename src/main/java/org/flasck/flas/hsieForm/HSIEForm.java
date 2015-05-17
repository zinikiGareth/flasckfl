package org.flasck.flas.hsieForm;

import java.util.ArrayList;
import java.util.List;

public class HSIEForm extends HSIEBlock {
	public static class Var {
		public final int idx;

		public Var(int i) {
			this.idx = i;
		}
		
		@Override
		public String toString() {
			return "v" + idx;
		}
	}

	private final List<Var> vars = new ArrayList<Var>();
	private final List<String> dependsOn = new ArrayList<String>();

	public HSIEForm(int nformal, int nbound, List<String> dependsOn) {
		for (int i=0;i<nformal;i++)
			vars.add(new Var(i));
		for (int i=0;i<nbound;i++)
			vars.add(new Var(nformal + i));
		this.dependsOn.addAll(dependsOn);
	}

	public Var var(int v) {
		return vars.get(v);
	}

	public void dump() {
		dump(0);
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
