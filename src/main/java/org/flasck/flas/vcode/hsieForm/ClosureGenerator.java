package org.flasck.flas.vcode.hsieForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.droidgen.DroidPushArgument;
import org.flasck.flas.generators.GenerationContext;
import org.slf4j.Logger;
import org.zinutils.bytecode.IExpr;

public interface ClosureGenerator {
	// TODO: I think I would like to remove this altogether and replace with just a "generate into a context" method.
	// I think that is the entire plan here ...
	public List<HSIEBlock> nestedCommands();
	public boolean justScoping();
	public IExpr arguments(GenerationContext cxt, DroidPushArgument dpa, int from);
	public List<VarInSource> dependencies();
	public void dumpOne(Logger logTo, int i);
	public void dumpOne(PrintWriter pw, int i);
}
