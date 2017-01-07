package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.android.AndroidLabel;
import org.flasck.flas.commonBase.android.AndroidLaunch;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.compiler.HSIEFormGenerator;
import org.flasck.flas.rewriter.CodeGenRegistry;
import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.bytecode.Annotation;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.exceptions.UtilException;

public class DroidGenerator implements RepoVisitor, HSIEFormGenerator {
	private final boolean doBuild;
	private ByteCodeStorage bce;

	public DroidGenerator(boolean doBuild, ByteCodeStorage bce) {
		this.doBuild = doBuild;
		this.bce = bce;
	}

	public void registerWith(CodeGenRegistry rewriter) {
		if (doBuild)
			rewriter.registerCodeGenerator(this);
	}

	@Override
	public TemplateGenerator templateGenerator() {
		return new DroidTemplateGenerator(doBuild, bce);
	}
	
	@Override
	public void visitStructDefn(RWStructDefn sd) {
		if (!doBuild || !sd.generate)
			return;
		ByteCodeSink bcc = bce.newClass(sd.name());
		bcc.generateAssociatedSourceFile();
		DroidStructFieldGenerator fg = new DroidStructFieldGenerator(bcc, Access.PUBLIC);
		sd.visitFields(fg);
		bcc.superclass(J.FLAS_OBJECT);
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.FLAS_OBJECT, "<init>").flush();
			ctor.returnVoid().flush();
		}
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_doFullEval");
		gen.returns("void");
		NewMethodDefiner dfe = gen.done();
		DroidStructFieldInitializer fi = new DroidStructFieldInitializer(dfe, fg.fields);
		sd.visitFields(fi);
		dfe.returnVoid().flush();
	}

	@Override
	public void visitContractDecl(RWContractDecl cd) {
		if (!doBuild)
			return;
		ByteCodeSink bcc = bce.newClass(cd.name());
		bcc.generateAssociatedSourceFile();
		bcc.superclass(J.CONTRACT_IMPL);
		bcc.makeAbstract();
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.CONTRACT_IMPL, "<init>").flush();
			ctor.returnVoid().flush();
		}
		
		for (RWContractMethodDecl m : cd.methods) {
			if (m.dir.equals("down")) {
				GenericAnnotator gm = GenericAnnotator.newMethod(bcc, false, m.name);
				gm.returns("java.lang.Object");
				int k = 0;
				for (@SuppressWarnings("unused") Object a : m.args)
					gm.argument("java.lang.Object", "arg"+(k++));
				gm.done();
			}
		}
	}

	public void visitCardGrouping(CardGrouping grp) {
		if (!doBuild)
			return;
		ByteCodeSink bcc = bce.newClass(grp.struct.name());
		bcc.generateAssociatedSourceFile();
		bcc.superclass(J.FLASCK_CARD);
		bcc.inheritsField(true, Access.PUBLIC, J.WRAPPER, "_wrapper");
		bcc.inheritsField(true, Access.PUBLIC, J.DISPLAY_ENGINE, "_display");
		grp.struct.visitFields(new DroidStructFieldGenerator(bcc, Access.PROTECTED));
		for (ContractGrouping x : grp.contracts) {
			if (x.referAsVar != null)
				bcc.defineField(false, Access.PROTECTED, new JavaType(DroidUtils.javaNestedName(x.implName.jsName())), x.referAsVar);
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), DroidUtils.javaNestedSimpleName(x.implName.jsName()));
		}
		for (HandlerGrouping h : grp.handlers) {
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), DroidUtils.javaNestedSimpleName(h.impl.handlerName.uniqueName()));
		}
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar despatcher = gen.argument(J.CARD_DESPATCHER, "despatcher");
			PendingVar engine = gen.argument(J.DISPLAY_ENGINE, "display");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.FLASCK_CARD, "<init>", despatcher.getVar(), engine.getVar()).flush();
			for (ContractGrouping x : grp.contracts) {
				IExpr impl = ctor.makeNew(DroidUtils.javaNestedName(x.implName.jsName()), ctor.myThis());
				if (x.referAsVar != null) {
					FieldExpr fe = ctor.getField(x.referAsVar);
					ctor.assign(fe, impl).flush();
					impl = fe;
				}
				ctor.callVirtual("void", ctor.myThis(), "registerContract", ctor.stringConst(x.contractName.uniqueName()), ctor.as(impl, J.CONTRACT_IMPL)).flush();
			}
			ctor.callSuper("void", J.FLASCK_CARD, "ready").flush();
			ctor.returnVoid().flush();
		}
		// TODO: I feel this should come from the "app" definition file, NOT the "platform" spec ...
		if (grp.platforms.containsKey("android")) {
			PlatformSpec spec = grp.platforms.get("android");
			for (Object d : spec.defns) {
				if (d instanceof AndroidLaunch)
					bcc.addRTVAnnotation("com.gmmapowell.quickbuild.annotations.android.MainActivity");
				else if (d instanceof AndroidLabel) {
					Annotation label = bcc.addRTVAnnotation("com.gmmapowell.quickbuild.annotations.android.Label");
					label.addParam("value", ((AndroidLabel)d).label);
				} else
					throw new UtilException("Cannot handle android platform spec of type " + d.getClass());
			}
		}
	}

	@Override
	public void visitContractImpl(RWContractImplements ci) {
		if (!doBuild)
			return;
		CSName name = (CSName) ci.realName;
		String un = name.uniqueName();
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(un));
		bcc.generateAssociatedSourceFile();
		bcc.superclass(ci.name());
		IFieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(DroidUtils.javaBaseName(un)), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(un), DroidUtils.javaNestedSimpleName(un));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(DroidUtils.javaBaseName(un), "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", ci.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
		
	}

	public void visitServiceImpl(RWContractService cs) {
		if (!doBuild)
			return;
		String name = cs.realName.uniqueName();
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(name));
		bcc.generateAssociatedSourceFile();
		bcc.superclass(cs.name());
		IFieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(DroidUtils.javaBaseName(name)), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(name), DroidUtils.javaNestedSimpleName(name));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(DroidUtils.javaBaseName(name), "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", cs.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
	}

	public void visitHandlerImpl(RWHandlerImplements hi) {
		if (!doBuild)
			return;
		String name = hi.handlerName.uniqueName();
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(name));
		bcc.generateAssociatedSourceFile();
		bcc.superclass(hi.name());
		IFieldInfo fi = null;
		if (hi.inCard)
			fi = bcc.defineField(false, Access.PRIVATE, new JavaType(DroidUtils.javaBaseName(name)), "_card");
		Map<String, IFieldInfo> fs = new TreeMap<>();
		for (Object o : hi.boundVars) {
			String var = ((HandlerLambda)o).var;
			IFieldInfo hli = bcc.defineField(false, Access.PRIVATE, new JavaType("java.lang.Object"), var);
			fs.put(var, hli);
		}
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(name), DroidUtils.javaNestedSimpleName(name));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = null;
			if (hi.inCard)
				cardArg = gen.argument("java.lang.Object", "card");
			Map<String, PendingVar> vm = new TreeMap<String, PendingVar>();
			for (Object o : hi.boundVars) {
				String var = ((HandlerLambda)o).var;
				PendingVar pvi = gen.argument(J.OBJECT, var);
				vm.put(var, pvi);
			}
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", hi.name(), "<init>").flush();
			if (hi.inCard)
				ctor.assign(fi.asExpr(ctor), ctor.castTo(ctor.callStatic(J.FLEVAL, J.OBJECT, "full", cardArg.getVar()), DroidUtils.javaBaseName(name))).flush();
			for (Object o : hi.boundVars) {
				String var = ((HandlerLambda)o).var;
				ctor.assign(fs.get(var).asExpr(ctor), ctor.callStatic(J.FLEVAL, J.OBJECT, "head", vm.get(var).getVar())).flush();
			}
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar cardArg = null;
			if (hi.inCard)
				cardArg = gen.argument(J.OBJECT, "card");
			PendingVar argsArg = gen.argument("[" + J.OBJECT, "args");
			gen.returns(J.OBJECT);
			NewMethodDefiner eval = gen.done();
			List<Expr> naList = new ArrayList<Expr>();
			if (hi.inCard)
				naList.add(cardArg.getVar());
			for (int k=0;k<hi.boundVars.size();k++)
				naList.add(eval.arrayElt(argsArg.getVar(), eval.intConst(k)));
			Expr[] newArgs = new Expr[naList.size()];
			naList.toArray(newArgs);
			IExpr objArg;
			if (hi.inCard)
				objArg = cardArg.getVar();
			else
				objArg = eval.aNull();
			eval.ifOp(0xa2, eval.arraylen(argsArg.getVar()), eval.intConst(hi.boundVars.size()), 
					eval.returnObject(eval.makeNew("org.flasck.jvm.FLCurry", objArg, eval.classConst(DroidUtils.javaNestedName(name)), argsArg.getVar())), 
					eval.returnObject(eval.makeNew(DroidUtils.javaNestedName(name), newArgs))).flush();
		}
	}

	@Override
	public void generate(HSIEForm form) {
		if (!doBuild)
			return;
		new DroidHSIEFormGenerator(bce, form).generate();
	}
}
