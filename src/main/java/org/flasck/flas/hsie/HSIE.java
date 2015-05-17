package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.hsieForm.ContainsScope;
import org.flasck.flas.hsieForm.FunctionDefinition;
import org.flasck.flas.hsieForm.HSIEForm;
import org.flasck.flas.hsieForm.Scope;
import org.flasck.flas.parsedForm.FunctionCaseDefn;

public class HSIE {
	public static void handle(FunctionDefinition defn) {
		List<FunctionCaseDefn> cases = defn.cases;
		HSIEForm form = new HSIEForm(0, 0, new ArrayList<String>());
		defn.setHSIE(form);
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
