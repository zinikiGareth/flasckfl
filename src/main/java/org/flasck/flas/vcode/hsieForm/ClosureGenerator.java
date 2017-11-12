package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.List;

import org.slf4j.Logger;

public interface ClosureGenerator {
	// TODO: I think I would like to remove this altogether and replace with just a "generate into a context" method.
	// I think that is the entire plan here ...
	public List<HSIEBlock> nestedCommands();
	public boolean justScoping();
	public <T> void arguments(ClosureHandler<T> h, int from, OutputHandler<T> handler);
	public List<VarInSource> dependencies();
	public void dumpOne(Logger logTo, int i);
	public void dumpOne(PrintWriter pw, int i);
}
