package org.flasck.flas.jsform;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class JSTarget {
	// TODO (transitional): shouldn't be public
	public final List<JSForm> forms = new ArrayList<JSForm>();
	private final String pkg;
	private final Set<String> havePackages = new TreeSet<String>();

	public JSTarget(String pkg) {
		this.pkg = pkg;
		ensurePackagesFor(pkg);
	}
	
	public void ensurePackagesFor(String pkg) {
		String keydot = pkg+".";
		int idx = -1;
		while ((idx = keydot.indexOf('.', idx+1))!= -1) {
			String tmp = keydot.substring(0, idx);
			if (!havePackages.contains(tmp))
				forms.add(JSForm.packageForm(tmp));
			havePackages.add(tmp);
		}
	}

	public void add(JSForm cf) {
		forms.add(cf);
	}

	public void writeTo(Writer w) throws IOException {
		for (JSForm js : forms) {
			js.writeTo(w);
			w.write("\n");
		}
		w.write(pkg + ";\n");
		w.flush();
	}
}
