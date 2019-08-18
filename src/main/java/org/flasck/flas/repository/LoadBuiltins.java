package org.flasck.flas.repository;

public class LoadBuiltins {

	public static void applyTo(Repository repository) {
		
		// Types
		new BuiltinRepositoryEntry("Any").loadInto(repository);
		new BuiltinRepositoryEntry("Card").loadInto(repository);
		new BuiltinRepositoryEntry("Croset").loadInto(repository);
		new BuiltinRepositoryEntry("List").loadInto(repository);
		new BuiltinRepositoryEntry("Map").loadInto(repository);
		new BuiltinRepositoryEntry("Number").loadInto(repository);
		new BuiltinRepositoryEntry("String").loadInto(repository);
		new BuiltinRepositoryEntry("Type").loadInto(repository);
		
		// Operators
		new BuiltinRepositoryEntry("+").loadInto(repository);
		
		// dubious backward compatibility
		
		new BuiltinRepositoryEntry("Crokeys").loadInto(repository);
		new BuiltinRepositoryEntry("Id").loadInto(repository);
	}

}
