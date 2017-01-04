package org.flasck.flas.droidgen;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.template.TemplateGenerator;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
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
	public void generateRender(String clz, AreaName areaName) {
		if (!doBuild)
			return;
		// TODO: I think we actually want the render function, but not sure :-)
		if (areaName == null)
			return;
		ByteCodeSink bcc = bce.get(clz);
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "render");
		PendingVar into = gen.argument("java.lang.String", "into");
		gen.returns("void");
		NewMethodDefiner render = gen.done();
		if (areaName != null) {
			String topBlock = areaName.javaName();
			render.makeNewVoid(DroidUtils.javaNestedName(topBlock), render.myThis(), (Expr)render.as(render.makeNew("org.flasck.android.areas.CardArea", render.getField(render.myThis(), "_wrapper"), (Expr)render.as(render.myThis(), J.FLASCK_ACTIVITY), into.getVar()), "org.flasck.android.areas.Area")).flush();
			bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(topBlock), DroidUtils.javaNestedSimpleName(topBlock));
		}
		render.returnVoid().flush();
	}

	@Override
	public CGRContext area(String clz, String base, String customTag) {
		if (!doBuild)
			return null;
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(clz));
		String baseClz = "org.flasck.android.areas." + base;
		bcc.superclass(baseClz);
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.Wrapper"), "_wrapper");
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.areas.Area"), "_parent");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(clz), DroidUtils.javaNestedSimpleName(clz));
		IFieldInfo card = bcc.defineField(true, Access.PRIVATE, DroidUtils.javaBaseName(clz), "_card");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(DroidUtils.javaBaseName(clz), "cardArg");
			PendingVar parent = gen.argument("org/flasck/android/areas/Area", "parent");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", baseClz, "<init>", parent.getVar(), customTag == null ? ctor.as(ctor.aNull(), "java.lang.String") : ctor.stringConst(customTag)).flush();
			ctor.assign(card.asExpr(ctor), cardArg.getVar()).flush();
			return new CGRContext(bcc, ctor, cardArg.getVar(), parent.getVar());
		}
	}
}
