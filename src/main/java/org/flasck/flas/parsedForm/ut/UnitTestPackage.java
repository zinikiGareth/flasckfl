package org.flasck.flas.parsedForm.ut;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.RepositoryEntry;

public class UnitTestPackage implements RepositoryEntry {
	private final UnitTestFileName utfn;
	private final List<UnitTestCase> tests = new ArrayList<>();

	public UnitTestPackage(UnitTestFileName utfn) {
		this.utfn = utfn;
	}
	
	public UnitTestFileName name() {
		return utfn;
	}

	public void testCase(UnitTestCase utc) {
		tests.add(utc);
	}

	public void data(UnitDataDeclaration data) {
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(this.toString());
	}

	public Iterable<UnitTestCase> tests() {
		return tests;
	}

}
