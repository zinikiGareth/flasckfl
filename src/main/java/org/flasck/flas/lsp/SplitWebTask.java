package org.flasck.flas.lsp;

import java.net.URI;
import org.flasck.flas.compiler.CompileUnit;
import org.zinutils.hfs.HFSFolder;

public class SplitWebTask implements Runnable {
	private CompileUnit compiler;
	private HFSFolder uifolder;
	private URI uri;
	private String text;

	public SplitWebTask(CompileUnit compiler, HFSFolder uifolder) {
		this.compiler = compiler;
		this.uifolder = uifolder;
	}

	public SplitWebTask(CompileUnit compiler, HFSFolder uifolder, URI uri, String text) {
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
			compiler.splitWeb(uifolder);
		}
	}

}
