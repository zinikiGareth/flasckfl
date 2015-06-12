package org.flasck.flas.jsform;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JSTarget {
	// TODO (transitional): shouldn't be public
	public final List<JSForm> forms = new ArrayList<JSForm>();
	private final String pkg;

	public JSTarget(String pkg) {
		this.pkg = pkg;
		String keydot = pkg+".";
		int idx = -1;
		while ((idx = keydot.indexOf('.', idx+1))!= -1) {
			String tmp = keydot.substring(0, idx);
			forms.add(JSForm.packageForm(tmp));
		}
	}
	
	public void add(JSForm cf) {
		forms.add(cf);
	}

	public void writeTo(FileWriter w) throws IOException {
		for (JSForm js : forms) {
			js.writeTo(w);
			w.write("\n");
		}
		w.write(pkg + ";\n");
	}
}
