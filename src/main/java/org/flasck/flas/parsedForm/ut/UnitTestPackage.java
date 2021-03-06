package org.flasck.flas.parsedForm.ut;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.RepositoryEntry;

public class UnitTestPackage implements RepositoryEntry {
	private final InputPosition loc;
	private final UnitTestFileName utfn;
	private final List<UnitTestCase> tests = new ArrayList<>();
	private final List<UnitDataDeclaration> decls = new ArrayList<>();

	public UnitTestPackage(InputPosition loc, UnitTestFileName utfn) {
		this.loc = loc;
		this.utfn = utfn;
	}
	
	@Override
	public InputPosition location() {
		return loc;
	}
	
	public UnitTestFileName name() {
		return utfn;
	}

	public void testCase(UnitTestCase utc) {
		tests.add(utc);
	}

	public void data(UnitDataDeclaration data) {
		decls.add(data);
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(this.toString());
	}

	public Iterable<UnitTestCase> tests() {
		return tests;
	}

	public Iterable<UnitDataDeclaration> decls() {
		return decls;
	}

}
