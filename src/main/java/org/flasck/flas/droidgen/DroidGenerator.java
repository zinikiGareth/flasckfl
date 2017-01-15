package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.android.AndroidLabel;
import org.flasck.flas.commonBase.android.AndroidLaunch;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.HandlerName;
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
import org.flasck.jvm.J;
import org.zinutils.bytecode.Annotation;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;

public class DroidGenerator implements RepoVisitor, HSIEFormGenerator {
	private final ByteCodeStorage bce;
	private final DroidHSIEFormGenerator formGen;
	private final DroidBuilder builder;

	public DroidGenerator(ByteCodeStorage bce, DroidBuilder builder) {
		this.bce = bce;
		this.builder = builder;
		this.formGen = new DroidHSIEFormGenerator(bce);
	}

	public void registerWith(CodeGenRegistry rewriter) {
		rewriter.registerCodeGenerator(this);
	}

	@Override
	public TemplateGenerator templateGenerator() {
		return new DroidTemplateGenerator(bce);
	}
	
	@Override
	public void visitStructDefn(RWStructDefn sd) {
		if (!sd.generate)
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
		ByteCodeSink bcc = bce.newClass(grp.struct.name());
		bcc.generateAssociatedSourceFile();
		bcc.superclass(J.FLASCK_CARD);
		bcc.inheritsField(true, Access.PUBLIC, J.WRAPPER, "_wrapper");
		bcc.inheritsField(true, Access.PUBLIC, J.DISPLAY_ENGINE, "_display");
		grp.struct.visitFields(new DroidStructFieldGenerator(bcc, Access.PROTECTED));
		for (ContractGrouping x : grp.contracts) {
			if (x.referAsVar != null)
				bcc.defineField(false, Access.PROTECTED, new JavaType(x.implName.javaClassName()), x.referAsVar);
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), x.implName.baseName());
		}
		for (HandlerGrouping h : grp.handlers) {
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), h.impl.handlerName.baseName);
		}
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar despatcher = gen.argument(J.CARD_DESPATCHER, "despatcher");
			PendingVar engine = gen.argument(J.DISPLAY_ENGINE, "display");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.FLASCK_CARD, "<init>", despatcher.getVar(), engine.getVar()).flush();
			for (ContractGrouping x : grp.contracts) {
				IExpr impl = ctor.makeNew(x.implName.javaClassName(), ctor.myThis());
				if (x.referAsVar != null) {
					IExpr fe = ctor.getField(x.referAsVar);
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
		CSName name = (CSName) ci.realName;
		String nn = name.javaClassName(); // nestedName
		String bn = name.containingCard().javaName(); // baseName
		String sn = name.baseName(); // nestedSimpleName
		ByteCodeSink bcc = bce.newClass(nn);
		bcc.generateAssociatedSourceFile();
		bcc.superclass(ci.name());
		IFieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(bn), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, bn, sn);
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(bn, "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", ci.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
	}

	public void visitServiceImpl(RWContractService cs) {
		CSName name = cs.realName;
		String nn = name.javaClassName(); // nestedName
		String cn = name.containingCard().javaName(); // baseName
		String bn = name.baseName(); // nestedSimpleName
		ByteCodeSink bcc = bce.newClass(nn);
		bcc.generateAssociatedSourceFile();
		bcc.superclass(cs.name());
		IFieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(cn), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, cn, bn);
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(cn, "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", cs.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
	}

	public void visitHandlerImpl(RWHandlerImplements hi) {
		HandlerName name = hi.handlerName;
		ByteCodeSink bcc = bce.newClass(name.javaClassName());
		bcc.generateAssociatedSourceFile();
		bcc.superclass(hi.name());
		IFieldInfo fi = null;
		if (hi.inCard)
			fi = bcc.defineField(false, Access.PRIVATE, new JavaType(name.containingCard().javaName()), "_card");
		Map<String, IFieldInfo> fs = new TreeMap<>();
		for (Object o : hi.boundVars) {
			String var = ((HandlerLambda)o).var;
			IFieldInfo hli = bcc.defineField(false, Access.PRIVATE, new JavaType("java.lang.Object"), var);
			fs.put(var, hli);
		}
		bcc.addInnerClassReference(Access.PUBLICSTATIC, name.name.uniqueName(), name.baseName);
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
				ctor.assign(fi.asExpr(ctor), ctor.castTo(ctor.callStatic(J.FLEVAL, J.OBJECT, "full", cardArg.getVar()), name.name.uniqueName())).flush();
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
					eval.returnObject(eval.makeNew("org.flasck.jvm.FLCurry", objArg, eval.classConst(name.javaClassName()), argsArg.getVar())), 
					eval.returnObject(eval.makeNew(name.javaClassName(), newArgs))).flush();
		}
	}

	@Override
	public void generate(HSIEForm form) {
		formGen.generate(form);
	}
}
