var test__golden = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";
import { root__package, InRoot, first, listLength } from "/js/root.package.js";

test__golden.x = function(_cxt) {
  const _v1 = _cxt.getSingleton('test.golden.x');
  if (_v1) return _v1;
  const _v2 = _cxt.array(4, 5);
  const _v3 = InRoot.eval(_cxt, _v2, 10);
  _cxt.cacheSingleton('test.golden.x', _v3);
  return _v3;
}

test__golden.x.nfargs = function() { return 0; }

test__golden.y = function(_cxt) {
  const _v1 = _cxt.getSingleton('test.golden.y');
  if (_v1) return _v1;
  const _v2 = listLength;
  const _v3 = test__golden.x;
  const _v4 = _cxt.closure(_v3);
  const _v5 = _cxt.mkacor(InRoot.prototype._field_x,_v4,0);
  const _v6 = _cxt.closure(_v2, _v5);
  _cxt.cacheSingleton('test.golden.y', _v6);
  return _v6;
}

test__golden.y.nfargs = function() { return 0; }

test__golden.z = function(_cxt) {
  const _v1 = _cxt.getSingleton('test.golden.z');
  if (_v1) return _v1;
  const _v2 = first;
  const _v3 = test__golden.x;
  const _v4 = _cxt.closure(_v3);
  const _v5 = _cxt.mkacor(InRoot.prototype._field_x,_v4,0);
  const _v6 = _cxt.closure(_v2, _v5);
  _cxt.cacheSingleton('test.golden.z', _v6);
  return _v6;
}

test__golden.z.nfargs = function() { return 0; }

test__golden._init = function(_cxt) {
  if (_cxt.isTruthy(test__golden._builtin_init)) {
    test__golden._builtin_init(_cxt);
  }
}

test__golden._init.nfargs = function() { return 0; }

export { test__golden };
