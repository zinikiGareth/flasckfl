package org.flasck.flas.repository;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.NamedThing;

public interface RepositoryEntry {

	NameOfThing name();
	void dumpTo(PrintWriter pw);

}
