var root__package = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";

var InRoot = function(_cxt) {
  this.state = _cxt.fields();
  this.state.set('_type', 'InRoot');
  return ;
}


InRoot._typename = 'InRoot'

InRoot.prototype._areYouA = function(_cxt, ty) {
  if (_cxt.isTruthy(ty == 'InRoot')) {
    return true;
  } else 
    return false;
}

InRoot.prototype._areYouA.nfargs = function() { return 1; }

InRoot.eval = function(_cxt, x, head) {
  const _v1 = new InRoot(_cxt);
  _v1.state.set('x', x);
  _v1.state.set('head', head);
  return _v1;
}

InRoot.eval.nfargs = function() { return 2; }

InRoot.prototype._field_head = function(_cxt) {
  return this.state.get('head');
}

InRoot.prototype._field_head.nfargs = function() { return 0; }

InRoot.prototype._field_x = function(_cxt) {
  return this.state.get('x');
}

InRoot.prototype._field_x.nfargs = function() { return 0; }

var first = function(_cxt, _0) {
  _0 = _cxt.head(_0);
  if (_cxt.isA(_0, 'Cons')) {
    var _1 = _cxt.field(_0, 'head');
    const x = _1;
    return x;
  } else if (_cxt.isA(_0, 'Nil')) {
    const _v1 = FLError.eval(_cxt, 'empty list');
    return _v1;
  } else 
    return FLError.eval(_cxt, 'first: no matching case');
}

first.nfargs = function() { return 1; }

var listLength = function(_cxt, _0) {
  _0 = _cxt.head(_0);
  if (_cxt.isA(_0, 'Cons')) {
    var _1 = _cxt.field(_0, 'tail');
    const x = _1;
    const _v1 = FLBuiltin.plus;
    const _v2 = listLength;
    const _v3 = _cxt.closure(_v2, x);
    const _v4 = _cxt.closure(_v1, 1, _v3);
    return _v4;
  } else if (_cxt.isA(_0, 'Nil')) {
    return 0;
  } else 
    return FLError.eval(_cxt, 'listLength: no matching case');
}

listLength.nfargs = function() { return 1; }

root__package._init = function(_cxt) {
  const _v1 = _cxt.registerStruct('InRoot', InRoot);
  if (_cxt.isTruthy(root__package._builtin_init)) {
    root__package._builtin_init(_cxt);
  }
}

root__package._init.nfargs = function() { return 0; }

export { InRoot, first, listLength, root__package };
