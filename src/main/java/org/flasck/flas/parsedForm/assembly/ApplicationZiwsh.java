package org.flasck.flas.parsedForm.assembly;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parser.assembly.ZiwshConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class ApplicationZiwsh implements ZiwshConsumer, RepositoryEntry {

	private final InputPosition location;
	private final AssemblyName name;
	private ExprToken wsuri;
	private ValidIdentifierToken secureModule;
	private ValidIdentifierToken secureClz;
	private ExprToken loginflow;

	public ApplicationZiwsh(InputPosition location, AssemblyName assemblyName) {
		this.location = location;
		this.name = assemblyName;
	}

	@Override
	public NameOfThing name() {
		return name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("ZIWSH");
	}

	@Override
	public void wsuri(ExprToken tok) {
		this.wsuri = tok;
	}

	@Override
	public void security(ValidIdentifierToken mod, ValidIdentifierToken clz) {
		this.secureModule = mod;
		this.secureClz = clz;
	}

	@Override
	public void loginflow(ExprToken tok) {
		this.loginflow = tok;
	}
}
