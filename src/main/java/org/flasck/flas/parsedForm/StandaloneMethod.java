package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.repository.RepositoryEntry;

public class StandaloneMethod implements RepositoryEntry {
	public final ObjectMethod om;

	public StandaloneMethod(ObjectMethod om) {
		this.om = om;
	}

	public FunctionName name() {
		return om.name();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	@Override
	public String toString() {
		return "StandaloneMethod[" + om + "]";
	}
}
