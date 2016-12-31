package org.flasck.flas.droidgen;

import org.flasck.jvm.ContractImpl;
import org.flasck.jvm.FLClosure;
import org.flasck.jvm.FLError;
import org.flasck.jvm.FLEval;
import org.flasck.jvm.Wrapper;
import org.flasck.jvm.builtin.Cons;
import org.flasck.jvm.builtin.Nil;
import org.flasck.jvm.builtin.Send;
import org.flasck.jvm.cards.CardDespatcher;
import org.flasck.jvm.cards.FlasckCard;
import org.flasck.jvm.display.DisplayEngine;
import org.flasck.jvm.post.DeliveryAddress;

public class J {

	public static final String CONTRACT_IMPL = ContractImpl.class.getName();
	public static final String CARD_DESPATCHER = CardDespatcher.class.getName();
	public static final String STRING = String.class.getName();
	public static final String SEND = Send.class.getName();
	public static final String CONS = Cons.class.getName();
	public static final String FLCLOSURE = FLClosure.class.getName();
	public static final String NIL = Nil.class.getName();
	public static final String FLERROR = FLError.class.getName();
	public static final String FLEVAL = FLEval.class.getName();
	public static final String OBJECT = Object.class.getName();
	public static final String DELIVERY_ADDRESS = DeliveryAddress.class.getName();
	public static final String DISPLAY_ENGINE = DisplayEngine.class.getName();
	public static final String WRAPPER = Wrapper.class.getName();
	public static final String FLASCK_CARD = FlasckCard.class.getName();

}
