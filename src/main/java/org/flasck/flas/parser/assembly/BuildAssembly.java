package org.flasck.flas.parser.assembly;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationZiwsh;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.flas.repository.Repository;

public class BuildAssembly implements AssemblyDefinitionConsumer {
	private final ErrorReporter errors;
	private final Repository repository;

	public BuildAssembly(ErrorReporter errors, Repository repository) {
		this.errors = errors;
		this.repository = repository;
	}

	@Override
	public void assembly(Assembly assembly) {
		repository.addEntry(errors, assembly.name(), assembly);
	}

	@Override
	public void routingTable(ApplicationRouting routing) {
		repository.addEntry(errors, routing.name(), routing);
	}

	@Override
	public void ziwshModel(ApplicationZiwsh ziwshModel) {
		repository.addEntry(errors, ziwshModel.name(), ziwshModel);
	}
}
