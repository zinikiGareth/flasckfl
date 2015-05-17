package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.parsedForm.ContainsScope;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.vcode.hsieForm.HSIEForm;

public class HSIE {
	public static HSIEForm handle(FunctionDefinition defn) {
		List<FunctionCaseDefn> cases = defn.cases;
		System.out.println(defn.nargs);
		return new HSIEForm(0, 0, new ArrayList<String>());
	}

	public static void applyTo(Scope s) {
		for (Entry<String, Object> x : s) {
			if (x instanceof FunctionDefinition) {
				handle((FunctionDefinition) x);
			}
			if (x instanceof ContainsScope)
				applyTo(((ContainsScope)x).innerScope());
		}
	}

}
