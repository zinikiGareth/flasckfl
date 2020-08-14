package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.exceptions.NotImplementedException;

public class HIGeneratorJS extends LeafAdapter {
	private NestedVisitor sv;
	private JSMethodCreator hdlrCtor;
	private JSClassCreator hdlr;
	private JSMethodCreator eval;
	private JSExpr evalRet;

	public HIGeneratorJS(NestedVisitor sv, JSStorage jse, Map<Object, List<FunctionName>> methodMap, HandlerImplements hi, StateHolder sh) {
		this.sv = sv;
		sv.push(this);
		
		HandlerName name = (HandlerName)hi.name();
		this.hdlr = jse.newClass(name.packageName().jsName(), name);
		this.hdlr.inheritsFrom(hi.actualType().name(), J.HANDLERBASE);
		this.hdlr.implementsJava(hi.actualType().name().javaName());
		this.hdlr.inheritsField(true, Access.PROTECTED, new PackageName(J.FIELDS_CONTAINER), "state");

		this.hdlrCtor = hdlr.constructor();
		JSVar cx = this.hdlrCtor.argument(J.FLEVALCONTEXT, "_cxt");
		this.hdlrCtor.stateField(true);
		this.hdlrCtor.superArg(cx);
		this.eval = hdlr.createMethod("eval", false);
		this.eval.argumentList();
		this.eval.argument("_cxt");
		List<JSExpr> args = new ArrayList<JSExpr>();
		if (sh != null) {
			hdlr.field(true, Access.PRIVATE, new PackageName(J.OBJECT), "_card");
			hdlrCtor.argument("_incard");
			hdlrCtor.setField(false, "_card", new JSVar("_incard"));
			this.eval.argumentList();
			this.eval.argument("_incard");
			args.add(new JSVar("_incard"));
		}
		this.evalRet = eval.newOf(hi.name(), args);
		this.eval.storeField(true, this.evalRet, "_type", this.eval.string(name.uniqueName()));
		jse.handler(hi);
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(hi, methods);
		jse.methodList(hi.name(), methods);

	}

	@Override
	public void visitHandlerLambda(HandlerLambda hl) {
		String name;
		if (hl.patt instanceof TypedPattern)
			name = ((TypedPattern)hl.patt).var.var;
		else
			throw new NotImplementedException("pattern " + hl);
		JSExpr arg = this.eval.argument(name);
		this.eval.storeField(false, this.evalRet, name, arg);
	}
	
	@Override
	public void leaveHandlerImplements(HandlerImplements hi) {
		eval.returnObject(evalRet);
		sv.result(null);
	}

}
