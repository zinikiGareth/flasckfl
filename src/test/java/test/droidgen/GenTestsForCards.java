package test.droidgen;

import java.util.ArrayList;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.parsedForm.StructDefn.StructType;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.PrimitiveType;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.ByteCodeStreamer;

public class GenTestsForCards {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStreamer bce = new ByteCodeStreamer();
	DroidGenerator gen = new DroidGenerator(bce, new DroidBuilder());
	
	IExpr expr = context.mock(IExpr.class);

	@Test
	public void testVisitingAnEmptyCardGeneratesTheCorrectMinimumCode() {
		CardGrouping sd = new CardGrouping(loc, new CardName(null, "Card"), new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true));
		gen.visitCardGrouping(sd);
		bce.expect("Card", "/gencards/minimal-card.txt");
		bce.validate();
	}

	@Test
	public void testVisitingACardWithOneDataMemberAndNoInitGeneratesAnEmptySlot() {
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Boolean")), "f1"));
		CardGrouping card = new CardGrouping(loc, new CardName(null, "Card"), sd);
		gen.visitCardGrouping(card);
		bce.expect("Card", "/gencards/card-member-no-init.txt");
		bce.validate();
	}

	@Test
	public void testVisitingACardWithOneInitializedMemberGeneratesASlotWithTheValue() {
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Number")), "f1", FunctionName.function(loc, null, "init_f1")));
		CardGrouping card = new CardGrouping(loc, new CardName(null, "Card"), sd);
		gen.visitCardGrouping(card);
		bce.expect("Card", "/gencards/card-member-with-init.txt");
		bce.validate();
	}

	@Test
	public void testCorrectGenerationOfContractWithNoVar() {
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(loc, cdName, sd);
		card.contracts.add(new ContractGrouping(new SolidName(null, "CtrDecl"), new CSName(cdName, "_C0"), null));
		gen.visitCardGrouping(card);
		bce.expect("Card", "/gencards/card-contract-no-var.txt");
		bce.validate();
	}

	@Test
	public void testCorrectGenerationOfContractWithVar() {
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(loc, cdName, sd);
		card.contracts.add(new ContractGrouping(new SolidName(null, "CtrDecl"), new CSName(cdName, "_C0"), "ce"));
		gen.visitCardGrouping(card);
		bce.expect("Card", "/gencards/card-contract-with-var.txt");
		bce.validate();
	}

	@Test
	public void testCorrectGenerationOfHandler() {
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(loc, cdName, sd);
		HandlerName hn = new HandlerName(cdName, "ActualHandler");
		card.handlers.add(new HandlerGrouping(new HandlerName(null, "ActualHandler"), new RWHandlerImplements(loc, loc, hn, new SolidName(null, "HandlerDecl"), true, new ArrayList<>())));
		gen.visitCardGrouping(card);
		bce.expect("Card", "/gencards/card-handler.txt");
		bce.validate();
	}
}
