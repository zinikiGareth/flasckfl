package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;

public class Provides extends Implements implements RepositoryEntry {
	public final String referAsVar;

	public Provides(InputPosition kw, InputPosition location, NamedType consumer, TypeReference type, CSName csn) {
		super(kw, location, consumer, type, csn);
		this.referAsVar = "_" + type.name();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.print("Provides[]");
	}
}
