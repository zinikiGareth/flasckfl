package org.flasck.flas.repository;

import java.io.PrintWriter;
import java.util.Comparator;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.st.SystemTest;

public interface RepositoryEntry {
	public enum ValidContexts {
		ALL, TESTS;
	}
	Comparator<? super RepositoryEntry> preferredOrder = new Comparator<RepositoryEntry>() {
		@Override
		public int compare(RepositoryEntry o1, RepositoryEntry o2) {
			int cat1 = category(o1);
			int cat2 = category(o2);
			if (cat1 < cat2)
				return -1;
			else if (cat1 > cat2)
				return 1;
			else
				return o1.name().uniqueName().compareTo(o2.name().uniqueName());
		}

		private int category(RepositoryEntry o1) {
			if (o1 instanceof ContractDecl)
				return 1;
			else if (o1 instanceof SystemTest)
				return 99;
			else
				return 50;
		}
	};
	
	NameOfThing name();
	InputPosition location();
	void dumpTo(PrintWriter pw);
	default ValidContexts validContexts() { return ValidContexts.ALL; }
}
