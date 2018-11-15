package test.droidgen;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.PrimitiveType;
import org.junit.Test;
import org.zinutils.bytecode.mock.ByteCodeStreamer;

public class GenTestsForEntities {
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStreamer bce = new ByteCodeStreamer();
	DroidGenerator gen = new DroidGenerator(bce, new DroidBuilder());

	@Test
	public void testNothingHappensIfWeDontWantToGenerateTheEntity() {
		RWStructDefn sd = new RWStructDefn(loc, FieldsDefn.FieldsType.ENTITY, new SolidName(null, "Entity"), false);
		gen.visitStructDefn(sd);
		bce.validate();
	}

	@Test
	public void testVisitingAnEmptyEntityGeneratesTheCorrectMinimumCode() {
		RWStructDefn sd = new RWStructDefn(loc, FieldsDefn.FieldsType.ENTITY, new SolidName(null, "Entity"), true);
		gen.visitStructDefn(sd);
		bce.expect("Entity", "/genentities/minimal-entity.txt");
		bce.validate();
	}

	@Test
	public void testVisitingAStructDefnWithOneMemberAndNoInitGeneratesAnEmptySlot() {
		RWStructDefn sd = new RWStructDefn(loc, FieldsDefn.FieldsType.ENTITY, new SolidName(null, "Entity"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Boolean")), "f1"));
		gen.visitStructDefn(sd);
		bce.expect("Entity", "/genentities/entity-with-field.txt");
		bce.validate();
	}

	@Test
	public void testVisitingAStructDefnWithOneInitializedMemberGeneratesASlotWithTheValue() {
		RWStructDefn sd = new RWStructDefn(loc, FieldsDefn.FieldsType.ENTITY, new SolidName(null, "Entity"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Number")), "f1", FunctionName.function(loc, null, "init_f1")));
		gen.visitStructDefn(sd);
		bce.expect("Entity", "/genentities/entity-with-field-init.txt");
		bce.validate();
	}
}
