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
	private URI uri;
	private String text;

	public SplitWebTask(ErrorReporter errors, CompileUnit compiler, HFSFolder uifolder) {
		this.errors = errors;
		this.compiler = compiler;
		this.uifolder = uifolder;
	}

	public SplitWebTask(ErrorReporter errors, CompileUnit compiler, HFSFolder uifolder, URI uri, String text) {
		this.errors = errors;
		this.compiler = compiler;
		this.uifolder = uifolder;
		this.uri = uri;
		this.text = text;
	}

	@Override
	public void run() {
		if (uri != null) {
			compiler.splitWebFile(uri, text);
		} else {
			errors.beginPhase2(uifolder.getPath());
			compiler.splitWeb(uifolder);
            List<URI> broken = new ArrayList<>();
            errors.doneProcessing(broken);
		}
	}

}
