package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;

public class ObjectMethod extends ObjectActionHandler {
	public ObjectMethod(InputPosition location, FunctionName name, List<Pattern> args) {
		super(location, name, args);
	}
	
	@Override
	public String toString() {
		return name().uniqueName() + "/" + args().size();
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("ObjectMethod[" + toString() + "]");
	}
}
