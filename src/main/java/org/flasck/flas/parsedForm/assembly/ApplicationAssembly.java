package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.parser.assembly.ApplicationElementConsumer;

public class ApplicationAssembly extends Assembly implements ApplicationElementConsumer {
	private String title;

	public ApplicationAssembly(InputPosition loc, AssemblyName assemblyName) {
		super(loc, assemblyName);
	}

	@Override
	public void title(String s) {
		this.title = s;
	}
	
	public String getTitle() {
		return title;
	}
}
