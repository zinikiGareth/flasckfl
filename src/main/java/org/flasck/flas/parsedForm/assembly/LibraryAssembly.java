package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.parser.assembly.LibraryElementConsumer;

public class LibraryAssembly extends Assembly implements LibraryElementConsumer {
	public LibraryAssembly(InputPosition loc, AssemblyName assemblyName) {
		super(loc, assemblyName);
	}
}
