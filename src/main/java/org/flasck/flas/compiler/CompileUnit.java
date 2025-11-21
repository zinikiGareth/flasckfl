package org.flasck.flas.compiler;

import java.net.URI;

import org.zinutils.hfs.HFSFolder;

public interface CompileUnit {
	public void parse(URI uri, String text);
	public void attemptRest(URI uri);
	public void taskQueue(TaskQueue taskQ);
	public void lspLoadFLIM(URI uri);
	public void splitWeb(HFSFolder cardsFolder);
	public void splitWebFile(URI name, String text);
}
