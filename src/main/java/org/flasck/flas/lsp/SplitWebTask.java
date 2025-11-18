package org.flasck.flas.lsp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.CompileUnit;
import org.flasck.flas.errors.ErrorReporter;
import org.zinutils.hfs.HFSFolder;

public class SplitWebTask implements Runnable {

	private ErrorReporter errors;
	private CompileUnit compiler;
	private HFSFolder uifolder;

	public SplitWebTask(ErrorReporter errors, CompileUnit compiler, HFSFolder uifolder) {
		this.errors = errors;
		this.compiler = compiler;
		this.uifolder = uifolder;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		errors.beginSplitterPhase(uifolder.getPath());
		compiler.splitWeb(uifolder);
		List<URI> broken = new ArrayList<>();
		errors.doneProcessing(broken);
	}

}
