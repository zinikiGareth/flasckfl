package org.flasck.flas.droidgen;

import org.flasck.jvm.ContractImpl;
import org.flasck.jvm.FLASObject;
import org.flasck.jvm.FLClosure;
import org.flasck.jvm.FLCurry;
import org.flasck.jvm.FLError;
import org.flasck.jvm.FLEval;
import org.flasck.jvm.Wrapper;
import org.flasck.jvm.areas.Area;
import org.flasck.jvm.areas.CardArea;
import org.flasck.jvm.areas.IArea;
import org.flasck.jvm.areas.ListArea;
import org.flasck.jvm.areas.TextArea;
import org.flasck.jvm.builtin.Cons;
import org.flasck.jvm.builtin.Crokey;
import org.flasck.jvm.builtin.Croset;
import org.flasck.jvm.builtin.Nil;
import org.flasck.jvm.builtin.Send;
import org.flasck.jvm.cards.CardDespatcher;
import org.flasck.jvm.cards.FlasckCard;
import org.flasck.jvm.display.DisplayEngine;
import org.flasck.jvm.display.EventHandler;
import org.flasck.jvm.post.DeliveryAddress;
import org.zinutils.bytecode.JavaType;

public class J {
	// Java Primitives
	public static final JavaType BOOLEANP = JavaType.boolean_;
	public static final JavaType INTP = JavaType.int_;

	// Java Objects
	public static final String CLASS = Class.class.getName();
	public static final String OBJECT = Object.class.getName();
	public static final String BOOLEAN = Boolean.class.getName();
	public static final String INTEGER = Integer.class.getName();
	public static final String STRING = String.class.getName();

	// Essential FLAS Things
	public static final String FLCLOSURE = FLClosure.class.getName();
	public static final String FLERROR = FLError.class.getName();
	public static final String FLEVAL = FLEval.class.getName();
	public static final String FLFIELD = FLEVAL + "$Field";
	public static final String FLCURRY = FLCurry.class.getName();
	public static final String CONTRACT_IMPL = ContractImpl.class.getName();

	// Other FLAS builtins
	public static final String BUILTINPKG = "org.flasck.jvm.builtin";
	public static final String CROKEY = Crokey.class.getName();
	public static final String CROSET = Croset.class.getName();
	public static final String CONS = Cons.class.getName();
	public static final String NIL = Nil.class.getName();

	public static final String SEND = Send.class.getName();

	// JVM defined things
	public static final String CARD_DESPATCHER = CardDespatcher.class.getName();
	public static final String DELIVERY_ADDRESS = DeliveryAddress.class.getName();
	public static final String DISPLAY_ENGINE = DisplayEngine.class.getName();
	public static final String WRAPPER = Wrapper.class.getName();
	public static final String HANDLER = EventHandler.class.getName();

	// JVM object things we inherit from or something
	public static final String FLASCK_CARD = FlasckCard.class.getName();
	public static final String FLAS_OBJECT = FLASObject.class.getName();

	// Areas
	public static final String AREAPKG = "org.flasck.jvm.areas.";
	public static final String IAREA = IArea.class.getName();
	public static final String AREA = Area.class.getName();
	public static final String TEXT_AREA = TextArea.class.getName();
	public static final String LIST_AREA = ListArea.class.getName();
	public static final String CARD_AREA = CardArea.class.getName();

}
