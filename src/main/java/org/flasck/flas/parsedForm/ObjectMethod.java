package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.zinutils.exceptions.NotImplementedException;

public class ObjectMethod extends ObjectActionHandler {
	private ObjectDefn od;

	public ObjectMethod(InputPosition location, FunctionName name, List<Pattern> args) {
		super(location, name, args);
	}
	
	public void bindToObject(ObjectDefn od) {
		this.od = od;
	}

	public ObjectDefn getObject() {
		if (od == null)
			throw new NotImplementedException("There is no object definition bound here");
		return od;
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
