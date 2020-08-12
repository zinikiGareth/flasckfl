package org.flasck.flas.compiler.jsgen.packaging;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.templates.EventPlacement.HandlerInfo;
import org.flasck.flas.compiler.templates.EventPlacement.TemplateTarget;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;

public class EventMap {
	private final NameOfThing cardName;
	private final EventTargetZones methods;

	public EventMap(NameOfThing cardName, EventTargetZones eventMethods) {
		this.cardName = cardName;
		this.methods = eventMethods;
	}

	public void write(IndentWriter iw) {
		iw.println(cardName.jsName() + ".prototype._eventHandlers = function() {");
		IndentWriter jw = iw.indent();
		jw.print("return {");
		IndentWriter kw = jw.indent();
		boolean isFirst = true;
		for (String t : methods.templateNames()) {
			if (!isFirst) {
				kw.println(",");
			} else
				kw.println("");
			
			isFirst = false;
			kw.print(new JSString(t).asVar());
			kw.print(" : [");
			IndentWriter lw = kw.indent();
			boolean ft = true;
			for (TemplateTarget tt : methods.targets(t)) {
				ft = writeHi(ft, lw, tt.type, tt.slot, tt.option, methods.getHandler(tt.handler), tt.evcond);
			}
			if (!ft)
				kw.println("");
			kw.println("]");
			jw.newline();
		}
		List<HandlerInfo> unbound = methods.unboundHandlers();
		if (!unbound.isEmpty()) {
			if (!isFirst) {
				kw.println(",");
			} else
				kw.println("");
			kw.print("_: [");
			boolean ft = true;
			IndentWriter lw = kw.indent();
			for (HandlerInfo hi : unbound) {
				ft = writeHi(ft, lw, null, null, 0, hi, null);
			}
			if (!ft)
				kw.println("");
			kw.println("]");
			jw.newline();
		}
		jw.println("};");
		iw.println("};");
	}

	private boolean writeHi(boolean ft, IndentWriter lw, String type, String slot, int option, HandlerInfo hi, Integer evcond) {
		if (!ft) {
			lw.println(",");
		} else
			lw.println("");
		lw.print("{ ");
		if (type != null) {
			lw.print("type: ");
			lw.print(new JSString(type).asVar());
			lw.print(", slot: ");
			lw.print(new JSString(slot).asVar());
			lw.print(", ");
			lw.print("option: ");
			lw.print(Integer.toString(option));
			lw.print(", ");
		}
		lw.print("event: ");
		lw.print(hi.event);
		lw.print(", handler: ");
		lw.print(hi.name.jsPName());
		if (evcond != null) {
			lw.print(", cond: ");
			lw.print(evcond.toString());
		}
		lw.print(" }");
		return false;
	}

	public void generate(ByteCodeEnvironment bce) {
		if (bce == null)
			return;
		
		ByteCodeSink bcc = bce.getOrCreate(cardName.javaName());
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_eventHandlers");
		gen.returns(Map.class.getName());
		NewMethodDefiner meth = gen.done();
		meth.lenientMode(true);
		Var v = meth.avar(Map.class.getName(), "ret");
		meth.assign(v, meth.makeNew(TreeMap.class.getName())).flush();
		
		for (String t : methods.templateNames()) {
			Var hl = meth.avar(List.class.getName(), "hl");
			meth.assign(hl, meth.makeNew(ArrayList.class.getName())).flush();
			for (TemplateTarget tt : methods.targets(t)) {
				HandlerInfo hi = methods.getHandler(tt.handler);
				IExpr classArgs = meth.arrayOf(Class.class.getName(), meth.classConst(J.FLEVALCONTEXT),
						meth.classConst("[L" + J.OBJECT + ";"));
				IExpr ehm = meth.callVirtual(Method.class.getName(),
						meth.classConst(cardName.javaName()), "getDeclaredMethod",
						meth.stringConst(hi.name.name), classArgs);

				IExpr ety, esl;
				if (tt.type != null) {
					ety = meth.stringConst(tt.type);
					esl = meth.stringConst(tt.slot);
				} else {
					ety = meth.as(meth.aNull(), J.STRING);
					esl = meth.as(meth.aNull(), J.STRING);
				}
				IExpr icond;
				if (tt.evcond != null) {
					icond = meth.box(meth.intConst(tt.evcond));
				} else
					icond = meth.as(meth.aNull(), J.INTEGER);
				IExpr ghi = meth.makeNew(J.HANDLERINFO, ety, esl, meth.box(meth.intConst(tt.option)), meth.stringConst(hi.event), ehm, icond);
				meth.voidExpr(meth.callInterface("boolean", hl, "add", meth.as(ghi, J.OBJECT))).flush();
			}
			meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst(t), J.OBJECT), meth.as(hl, J.OBJECT))).flush();
		}
		
		for (HandlerInfo hi : methods.unboundHandlers()) {
			Var hl = meth.avar(List.class.getName(), "hl");
			meth.assign(hl, meth.makeNew(ArrayList.class.getName())).flush();
			IExpr classArgs = meth.arrayOf(Class.class.getName(), meth.classConst(J.FLEVALCONTEXT),
					meth.classConst("[L" + J.OBJECT + ";"));
			IExpr ehm = meth.callVirtual(Method.class.getName(), meth.classConst(cardName.javaName()),
					"getDeclaredMethod", meth.stringConst(hi.name.name), classArgs);
			IExpr ghi = meth.makeNew(J.HANDLERINFO, meth.as(meth.aNull(), J.STRING),
					meth.as(meth.aNull(), J.STRING), meth.as(meth.aNull(), J.INTEGER),
					meth.stringConst(hi.event), ehm, meth.as(meth.aNull(), J.INTEGER));
			meth.voidExpr(meth.callInterface("boolean", hl, "add", meth.as(ghi, J.OBJECT))).flush();
			meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("_"), J.OBJECT), meth.as(hl, J.OBJECT))).flush();
		}
		
		meth.returnObject(v).flush();
	}
}
