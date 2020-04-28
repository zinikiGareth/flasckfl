package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parser.assembly.ApplicationElementConsumer;

public class ApplicationAssembly extends Assembly implements ApplicationElementConsumer {
	private String title;
	private String mainCard;

	public ApplicationAssembly(InputPosition loc, AssemblyName assemblyName) {
		super(loc, assemblyName);
	}

	@Override
	public void title(String s) {
		this.title = s;
	}
	
	public void mainCard(String s) {
		// TODO: should handle this as an unresolved type name and then pull back the real name later
		// otherwise we can reference things that don't exist
		this.mainCard = new CardName((PackageName) assemblyName.container(), s).uniqueName();
	}
	
	public String getTitle() {
		return title;
	}

	public String mainCard() {
		return mainCard;
	}
}
