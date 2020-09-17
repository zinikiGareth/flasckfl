package org.flasck.flas.parsedForm.st;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.parser.st.SystemTestDefinitionConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

public class SystemTest implements SystemTestDefinitionConsumer, RepositoryEntry {
	private final UnitTestFileName stfn;
	public final List<SystemTestStage> stages = new ArrayList<>();

	public SystemTest(UnitTestFileName stfn) {
		this.stfn = stfn;
	}

	@Override
	public void configure(SystemTestConfiguration utc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void test(SystemTestStage stage) {
		stages.add(stage);
	}

	@Override
	public void cleanup(SystemTestCleanup utc) {
		// TODO Auto-generated method stub
		
	}

	public NameOfThing name() {
		return stfn;
	}

	@Override
	public InputPosition location() {
		throw new NotImplementedException();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("SystemTest[" + stfn.uniqueName() + "]");
	}
}
