package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.RepositoryEntry;

public class HandlerLambda implements RepositoryEntry {
	public final Pattern patt;

	public HandlerLambda(Pattern patt) {
		this.patt = patt;
	}

	@Override
	public InputPosition location() {
		return patt.location();
	}
	
	@Override
	public NameOfThing name() {
		return ((RepositoryEntry)patt).name();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("HandlerLambda[" + patt + "]");
	}

}
