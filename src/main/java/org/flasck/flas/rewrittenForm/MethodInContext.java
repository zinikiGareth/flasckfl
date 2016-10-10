package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewriter.Rewriter.NamingContext;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class MethodInContext {
	public static final int DOWN = 1;
	public static final int UP = 2;
	public static final int EVENT = 3;
	public static final int OBJECT = 4;
	public static final int STANDALONE = 5;
	public final Scope scope;
	public final String fromContract;
	public final InputPosition contractLocation;
	public final int direction;
	public final String name;
	public final CodeType type;
	public final MethodDefinition method;
	public final List<Object> enclosingPatterns = new ArrayList<Object>();

	public MethodInContext(Rewriter rw, NamingContext cx, Scope scope, int dir, InputPosition cloc, String fromContract, String name, CodeType type, MethodDefinition method) {
		this.scope = scope;
		this.direction = dir;
		this.contractLocation = cloc;
		this.fromContract = fromContract;
		this.name = name;
		this.type = type;
		this.method = method;
		gatherEnclosing(rw, cx, scope);
	}

	private void gatherEnclosing(Rewriter rw, NamingContext cx, Scope s) {
		if (s == null)
			return;
		if (s.container != null) {
			gatherEnclosing(rw, cx, s.outer);
			Object ctr = s.container;
			if (ctr instanceof FunctionCaseDefn) { // Surely this should be a function case defn?
				FunctionCaseDefn fn = (FunctionCaseDefn)ctr;
				for (Object o : fn.intro.args) {
					enclosingPatterns.add(rw.rewritePattern(cx, o));
				}
			}
		}
	}
}
