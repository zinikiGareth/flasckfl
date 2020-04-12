package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.RepositoryEntry;

public class HandlerLambda implements RepositoryEntry {
	public final Pattern patt;

	public HandlerLambda(Pattern patt) {
		this.patt = patt;
	}

	@Override
	public NameOfThing name() {
		return ((RepositoryEntry)patt).name();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("HandlerLambda");
	}

}
