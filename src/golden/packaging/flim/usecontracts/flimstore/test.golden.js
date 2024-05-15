var test__golden = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";
import { test__contract } from "/js/test.contract.js";

test__golden.Mumble = function(_cxt) {
  FLCard.call(this, _cxt, null);
  this._contracts = new ContractStore(_cxt);
  this.state = _cxt.fields();
  this._contracts.require(_cxt, 'svc', 'test.contract.ForService');
  this._contracts.record(_cxt, 'test.contract.Simple', new test__golden.Mumble._C1(_cxt, this));
  return ;
}
test__golden.Mumble.prototype = new FLCard();
test__golden.Mumble.prototype.constructor = test__golden.Mumble;

test__golden.Mumble.prototype._updateDisplay = function(_cxt, _renderTree) {
  return ;
}

test__golden.Mumble.prototype._updateDisplay.nfargs = function() { return 1; }

test__golden.Mumble.prototype.name = function(_cxt) {
  return 'test.golden.Mumble';
}

test__golden.Mumble.prototype.name.nfargs = function() { return 0; }

test__golden.Mumble._contract = function(_cxt, _ctr) {
}

test__golden.Mumble._contract.nfargs = function() { return 1; }

test__golden.Mumble._C1 = function(_cxt, _incard) {
  this._card = _incard;
  return ;
}

test__golden.Mumble.Fred = function(_cxt, _incard) {
  test__contract.NotifyMe.call(this, _cxt);
  this.state = _cxt.fields();
  this._card = _incard;
  return ;
}
test__golden.Mumble.Fred.prototype = new test__contract.NotifyMe();
test__golden.Mumble.Fred.prototype.constructor = test__golden.Mumble.Fred;

test__golden.Mumble.Fred.eval = function(_cxt, _incard) {
  const _v1 = new test__golden.Mumble.Fred(_cxt, _incard);
  _v1.state.set('_type', 'test.golden.Mumble.Fred');
  return _v1;
}

test__golden.Mumble.Fred.eval.nfargs = function() { return 1; }

test__golden.Mumble.Fred.prototype._clz = function() {
  return 'test.contract.NotifyMe';
}

test__golden.Mumble.Fred.prototype._clz.nfargs = function() { return -1; }

test__golden.Mumble.Fred.prototype._card = function() {
  return this._card;
}

test__golden.Mumble.Fred.prototype._card.nfargs = function() { return -1; }

test__golden.Mumble.Fred.prototype.callback = function(_cxt, _0, _1) {
  const x = _0;
  const _v1 = FLBuiltin.show;
  const _v2 = _cxt.closure(_v1, x);
  const _v3 = Debug.eval(_cxt, _v2);
  const _v4 = _cxt.array(_v3);
  return _v4;
}

test__golden.Mumble.Fred.prototype.callback.nfargs = function() { return 2; }

test__golden.Mumble._C1.prototype.doit = function(_cxt, _0) {
  const _v1 = test__golden.Mumble.Fred.eval(_cxt, this._card);
  const _v2 = _cxt.mksend('mumble',this._card._contracts.required(_cxt, 'svc'),1,_v1,null);
  const _v3 = Nil.eval(_cxt);
  const _v4 = _cxt.closure(_v2, _v3);
  const _v5 = _cxt.array(_v4);
  return _v5;
}

test__golden.Mumble._C1.prototype.doit.nfargs = function() { return 1; }

test__golden._init = function(_cxt) {
  const _v1 = _cxt.registerStruct('test.golden.Mumble.Fred', test__golden.Mumble.Fred);
  if (_cxt.isTruthy(test__golden._builtin_init)) {
    test__golden._builtin_init(_cxt);
  }
}

test__golden._init.nfargs = function() { return 0; }
test__golden.Mumble.prototype._methods = function() {
  return {
  };
};
test__golden.Mumble._C1.prototype._methods = function() {
  return {
    "doit": test__golden.Mumble._C1.prototype.doit
  };
};
test__golden.Mumble.Fred.prototype._methods = function() {
  return {
    "callback": test__golden.Mumble.Fred.prototype.callback
  };
};
test__golden.Mumble.prototype._eventHandlers = function() {
  return {};
};

export { test__golden };
