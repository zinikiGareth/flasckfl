package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.compiler.HSIEFormGenerator;
import org.flasck.flas.parsedForm.StructDefn.StructType;
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
import org.flasck.flas.rewrittenForm.RWEventHandler;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.types.TypeWithName;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.jvm.J;
import org.flasck.jvm.fl.FLCurry;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

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
		String base = sd.ty == StructType.STRUCT?J.FLAS_OBJECT:J.FLAS_ENTITY; 
		bcc.superclass(base);
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			IExpr[] args = new IExpr[0];
			if (sd.ty == StructType.ENTITY)
				args = new IExpr[] { ctor.as(ctor.aNull(), J.BACKING_DOCUMENT) };
			ctor.callSuper("void", base, "<init>", args).flush();
			ctor.returnVoid().flush();
		}
		if (sd.ty == StructType.ENTITY) {
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar doc = gen.argument(J.BACKING_DOCUMENT, "doc");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", base, "<init>", doc.getVar()).flush();
			ctor.returnVoid().flush();
		}
		
		if (!sd.fields.isEmpty()) { // generate an arguments constructor
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar pv = gen.argument("[java.lang.Object", "args");
			gen.returns(sd.name());
			MethodDefiner meth = gen.done();
			Var v = pv.getVar();
			Var ret = meth.avar(sd.name(), "ret");
			meth.assign(ret, meth.makeNew(sd.name())).flush();
			int ap = 0;
			for (int i=0;i<sd.fields.size();i++) {
				RWStructField fld = sd.fields.get(i);
				if (fld.name.equals("id"))
					continue;
				meth.assign(meth.getField(ret, fld.name), meth.arrayElt(v, meth.intConst(ap++))).flush();
			}
			meth.returnObject(ret).flush();
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
		// Create the basic class
		ByteCodeSink top = bce.newClass(cd.name());
		generateContractImplPartForCard(cd, top);
		generateContractInterfaceForHandler(cd, top);
		generateContractInterfaceForService(cd, top);
	}

	public void generateContractImplPartForCard(RWContractDecl cd, ByteCodeSink parent) {
		ByteCodeSink bcc = bce.newClass(cd.name() + "$Impl");
		parent.addInnerClassReference(Access.PUBLICABSTRACTSTATIC, parent.getCreatedName(), "Impl");
		bcc.generateAssociatedSourceFile();
		bcc.superclass(J.CONTRACT_IMPL);
		bcc.implementsInterface(cd.name() + "$Down");
		bcc.makeAbstract();
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar des = gen.argument(J.IDESPATCHER, "despatcher");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.CONTRACT_IMPL, "<init>", des.getVar()).flush();
			ctor.returnVoid().flush();
		}
		
		for (RWContractMethodDecl m : cd.methods) {
			if (m.dir.equals("down")) {
				GenericAnnotator gm = GenericAnnotator.newMethod(bcc, false, m.name);
				gm.returns(J.OBJECT);
				gm.argument(J.OBJECT, "from");
				int k = 0;
				for (Object a : m.args) {
					generateArgument(gm, a, RWMethodDefinition.DOWN, k);
					k++;
				}
				gm.done();
			}
		}
	}

	public void generateContractInterfaceForService(RWContractDecl cd, ByteCodeSink parent) {
		ByteCodeSink bcc = bce.newClass(cd.name()+"$Up");
		parent.addInnerClassReference(Access.PUBLICSTATICINTERFACE, parent.getCreatedName(), "Up");
		bcc.generateAssociatedSourceFile();
		bcc.makeInterface();
		bcc.addInnerClassReference(Access.PUBLICSTATICINTERFACE, parent.getCreatedName(), "Up");
		bcc.implementsInterface("org.ziniki.ziwsh.UpContract");
		
		for (RWContractMethodDecl m : cd.methods) {
			if (m.dir.equals("up")) {
				GenericAnnotator gm = GenericAnnotator.newMethod(bcc, false, m.name);
				gm.returns("java.lang.Object");
				gm.argument(J.OBJECT, "from");
				int k = 0;
				for (Object a : m.args) {
					generateArgument(gm, a, RWMethodDefinition.DOWN, k);
					k++;
				}
				gm.done();
			}
		}
	}

	public void generateContractInterfaceForHandler(RWContractDecl cd, ByteCodeSink parent) {
		ByteCodeSink bcc = bce.newClass(cd.name()+"$Down");
		parent.addInnerClassReference(Access.PUBLICSTATICINTERFACE, parent.getCreatedName(), "Down");
		bcc.generateAssociatedSourceFile();
		bcc.makeInterface();
		bcc.implementsInterface("org.ziniki.ziwsh.DownContract");
		bcc.addInnerClassReference(Access.PUBLICSTATICINTERFACE, parent.getCreatedName(), "Down");
		
		for (RWContractMethodDecl m : cd.methods) {
			if (m.dir.equals("down")) {
				GenericAnnotator gm = GenericAnnotator.newMethod(bcc, false, m.name);
				gm.returns("java.lang.Object");
				gm.argument(J.OBJECT, "from");
				int k = 0;
				for (Object a : m.args) {
					generateArgument(gm, a, RWMethodDefinition.UP, k);
					k++;
				}
				gm.done();
			}
		}
	}

	public void generateArgument(GenericAnnotator gm, Object a, int dir, int k) {
		if (a instanceof RWTypedPattern) {
			TypeWithName type = ((RWTypedPattern)a).type;
			JavaType ty = JvmTypeMapper.map(type);
			if (type instanceof RWContractDecl) {
				ty = new JavaType(ty.getActual()+"$" + (dir == RWMethodDefinition.DOWN?"Down":"Up"));
			}
			gm.argument(ty, ((RWTypedPattern) a).var.var);
		} else if (a instanceof RWVarPattern) {
			gm.argument(J.OBJECT, ((RWVarPattern)a).var.var);
		} else
			gm.argument(J.OBJECT, "arg"+k);
	}

	public void visitCardGrouping(CardGrouping grp) {
		ByteCodeSink bcc = bce.newClass(grp.struct.name());
		bcc.generateAssociatedSourceFile();
		bcc.superclass(J.FLASCK_CARD);
		bcc.inheritsField(true, Access.PUBLIC, J.WRAPPER, "_wrapper");
		bcc.inheritsField(true, Access.PUBLIC, J.DISPLAY_ENGINE, "_display");
//		grp.struct.visitFields(new DroidStructFieldGenerator(bcc, Access.PROTECTED));
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
		for (RWEventHandler action : grp.areaActions)
			visitEventConnector(action);
		
		// TODO: we probably want *a* spec here, but the android one probably wants
		// to "include" an AndroidActivitySpec defined in JVMBuilder that we can pass straight over
		PlatformSpec spec = null;
		if (grp.platforms.containsKey("android")) {
			spec = grp.platforms.get("android");
//			for (Object d : spec.defns) {
//				if (d instanceof AndroidLaunch)
//					bcc.addRTVAnnotation("com.gmmapowell.quickbuild.annotations.android.MainActivity");
//				else if (d instanceof AndroidLabel) {
//					Annotation label = bcc.addRTVAnnotation("com.gmmapowell.quickbuild.annotations.android.Label");
//					label.addParam("value", ((AndroidLabel)d).label);
//				} else
//					throw new UtilException("Cannot handle android platform spec of type " + d.getClass());
//			}
		}
		CardName cn = grp.getName();
		builder.recordCard(cn.pkg == null ? null : cn.pkg.simpleName(), cn.cardName, spec);
	}

	@Override
	public void visitContractImpl(RWContractImplements ci) {
		CSName name = (CSName) ci.realName;
		String nn = name.javaClassName(); // nestedName
		String bn = name.containingCard().javaName(); // baseName
		String sn = name.baseName(); // nestedSimpleName
		ByteCodeSink bcc = bce.newClass(nn);
		bcc.generateAssociatedSourceFile();
		bcc.superclass(ci.name() + "$Impl");
		IFieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(bn), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, bn, sn);
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(bn, "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", ci.name(), "<init>", ctor.callVirtual(J.IDESPATCHER, cardArg.getVar(), "getDespatcher")).flush();
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
		bcc.superclass(cs.name() + "$Impl");
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
		bcc.superclass(hi.name() + "$Impl");
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
			if (hi.inCard) {
				ctor.callSuper("void", hi.name(), "<init>", ctor.callVirtual(J.IDESPATCHER, ctor.castTo(ctor.callStatic(J.FLEVAL, J.OBJECT, "full", cardArg.getVar()), name.name.uniqueName()), "getDespatcher")).flush();
				ctor.assign(fi.asExpr(ctor), ctor.castTo(ctor.callStatic(J.FLEVAL, J.OBJECT, "full", cardArg.getVar()), name.name.uniqueName())).flush();
			} else {
				ctor.callSuper("void", hi.name(), "<init>").flush();
			}
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
			List<IExpr> naList = new ArrayList<>();
			if (hi.inCard)
				naList.add(cardArg.getVar());
			for (int k=0;k<hi.boundVars.size();k++)
				naList.add(eval.arrayElt(argsArg.getVar(), eval.intConst(k)));
			IExpr[] newArgs = new IExpr[naList.size()];
			naList.toArray(newArgs);
			IExpr objArg;
			if (hi.inCard)
				objArg = cardArg.getVar();
			else
				objArg = eval.aNull();
			final IExpr makeIt = eval.returnObject(eval.makeNew(name.javaClassName(), newArgs));
			if (hi.boundVars.size() > 0)
				eval.ifOp(0xa2, eval.arraylen(argsArg.getVar()), eval.intConst(hi.boundVars.size()), 
						eval.returnObject(eval.makeNew(FLCurry.class.getName(), objArg, eval.classConst(name.javaClassName()), argsArg.getVar())), 
						makeIt).flush();
			else
				makeIt.flush();
		}
	}

	public void visitEventConnector(RWEventHandler aa) {
		ByteCodeSink bcc = bce.newClass(aa.handlerFn.javaClassName());
		bcc.generateAssociatedSourceFile();
		bcc.superclass("java.lang.Object");
		bcc.implementsInterface(J.HANDLER);
		String cardClz = aa.handlerFn.containingCard().javaName();
		bcc.defineField(true, Access.PROTECTED, cardClz, "_card");
		{
			GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
			PendingVar card = ann.argument(J.OBJECT, "card");
			MethodDefiner ctor = ann.done();
			ctor.callSuper("void", J.OBJECT, "<init>").flush();
			ctor.assign(ctor.getField("_card"), ctor.castTo(card.getVar(), cardClz)).flush();
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "handle");
			PendingVar evP = ann.argument(new JavaType(J.OBJECT), "ev");
			ann.returns(JavaType.object_);
			NewMethodDefiner meth = ann.done();
			meth.returnObject(meth.makeNew(J.FLCLOSURE, meth.as(meth.getField("_card"), J.OBJECT), meth.callVirtual(J.OBJECT, meth.myThis(), "getHandler"), meth.arrayOf(J.OBJECT, Arrays.asList(evP.getVar())))).flush();
		}
	}

	@Override
	public void generate(HSIEForm form) {
		formGen.generate(form);
	}
}
