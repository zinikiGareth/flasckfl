package org.flasck.flas.droidgen;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;

public class DroidTemplateGenerator implements TemplateGenerator {
	private final boolean doBuild;
	private ByteCodeStorage bce;

	public DroidTemplateGenerator(boolean doBuild, ByteCodeStorage bce) {
		this.doBuild = doBuild;
		this.bce = bce;
	}

	@Override
	public void generateRender(TemplateName tname, AreaName areaName) {
		if (!doBuild)
			return;
		// TODO: I think we actually want the render function, but not sure :-)
		if (areaName == null)
			return;
		ByteCodeSink bcc = bce.get(tname.uniqueName());
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "render");
		PendingVar into = gen.argument("java.lang.String", "into");
		gen.returns("void");
		NewMethodDefiner render = gen.done();
		if (areaName != null) {
			String topBlock = areaName.javaName();
			IExpr cardArea = render.makeNew(J.CARD_AREA, render.getField(render.myThis(), "_wrapper"), render.getField(render.myThis(), "_display"), into.getVar());
			render.makeNewVoid(DroidUtils.javaNestedName(topBlock), render.myThis(), render.as(cardArea, J.AREA)).flush();
			bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(topBlock), DroidUtils.javaNestedSimpleName(topBlock));
		}
		render.returnVoid().flush();
	}

	@Override
	public DroidAreaGenerator area(AreaName areaName, String base, String customTag, String nsTag, Object wantCard, Object wantYoyo) {
		if (!doBuild)
			return null;
		String clz = areaName.javaName();
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(clz));
		bcc.generateAssociatedSourceFile();
		String baseClz = J.AREAPKG + base;
		bcc.superclass(baseClz);
		bcc.inheritsField(false, Access.PUBLIC, new JavaType(J.WRAPPER), "_wrapper");
		bcc.inheritsField(false, Access.PUBLIC, new JavaType(J.AREA), "_parent");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(clz), DroidUtils.javaNestedSimpleName(clz));
		IFieldInfo card = bcc.defineField(true, Access.PRIVATE, DroidUtils.javaBaseName(clz), "_card");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(DroidUtils.javaBaseName(clz), "cardArg");
			PendingVar parent = gen.argument(J.AREA, "parent");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", baseClz, "<init>", parent.getVar(), customTag == null ? ctor.as(ctor.aNull(), "java.lang.String") : ctor.stringConst(customTag)).flush();
			ctor.assign(card.asExpr(ctor), cardArg.getVar()).flush();
			return new DroidAreaGenerator(bcc, ctor, cardArg.getVar(), parent.getVar());
		}
	}
}
