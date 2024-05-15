var test__contract = {};

import { IdempotentHandler } from "/js/ziwsh.js";
import { Application, Assign, AssignCons, AssignItem, Debug, ResponseWithMessages, Send, UpdateDisplay, ClickEvent, ScrollTo, ContractStore, Entity, Image, Link, FLBuiltin, False, True, MakeHash, HashPair, Tuple, TypeOf, FLCard, FLObject, FLError, Nil, Cons, Crobag, CroEntry, SlideWindow, CrobagWindow, CrobagChangeEvent, CrobagWindowEvent, Random, Interval, Instant, Calendar } from "/js/flasjs.js";

test__contract.ForService = function(_cxt) {
  return ;
}
test__contract.ForService._nf_mumble = 1;

test__contract.ForService.prototype.name = function() {
  return 'test.contract.ForService';
}

test__contract.ForService.prototype.name.nfargs = function() { return -1; }

test__contract.ForService.prototype._methods = function() {
  const _v1 = ['mumble'];
  return _v1;
}

test__contract.ForService.prototype._methods.nfargs = function() { return -1; }

test__contract.ForService.prototype.mumble = function(_cxt, _0, _ih) {
  return null;
}

test__contract.ForService.prototype.mumble.nfargs = function() { return 1; }

test__contract.NotifyMe = function(_cxt) {
  IdempotentHandler.call(this);
  return ;
}
test__contract.NotifyMe.prototype = new IdempotentHandler();
test__contract.NotifyMe.prototype.constructor = test__contract.NotifyMe;
test__contract.NotifyMe._nf_success = 0;
test__contract.NotifyMe._nf_failure = 1;
test__contract.NotifyMe._nf_callback = 1;

test__contract.NotifyMe.prototype.name = function() {
  return 'test.contract.NotifyMe';
}

test__contract.NotifyMe.prototype.name.nfargs = function() { return -1; }

test__contract.NotifyMe.prototype._methods = function() {
  const _v1 = ['success','failure','callback'];
  return _v1;
}

test__contract.NotifyMe.prototype._methods.nfargs = function() { return -1; }

test__contract.NotifyMe.prototype.success = function(_cxt, _ih) {
  return null;
}

test__contract.NotifyMe.prototype.success.nfargs = function() { return 0; }

test__contract.NotifyMe.prototype.failure = function(_cxt, _0, _ih) {
  return null;
}

test__contract.NotifyMe.prototype.failure.nfargs = function() { return 1; }

test__contract.NotifyMe.prototype.callback = function(_cxt, _0, _ih) {
  return null;
}

test__contract.NotifyMe.prototype.callback.nfargs = function() { return 1; }

test__contract.Simple = function(_cxt) {
  return ;
}
test__contract.Simple._nf_doit = 0;

test__contract.Simple.prototype.name = function() {
  return 'test.contract.Simple';
}

test__contract.Simple.prototype.name.nfargs = function() { return -1; }

test__contract.Simple.prototype._methods = function() {
  const _v1 = ['doit'];
  return _v1;
}

test__contract.Simple.prototype._methods.nfargs = function() { return -1; }

test__contract.Simple.prototype.doit = function(_cxt, _ih) {
  return null;
}

test__contract.Simple.prototype.doit.nfargs = function() { return 0; }

test__contract._init = function(_cxt) {
  const _v1 = new test__contract.ForService(_cxt);
  const _v2 = _cxt.registerContract('test.contract.ForService', _v1);
  const _v3 = new test__contract.NotifyMe(_cxt);
  const _v4 = _cxt.registerContract('test.contract.NotifyMe', _v3);
  const _v5 = new test__contract.Simple(_cxt);
  const _v6 = _cxt.registerContract('test.contract.Simple', _v5);
  if (_cxt.isTruthy(test__contract._builtin_init)) {
    test__contract._builtin_init(_cxt);
  }
}

test__contract._init.nfargs = function() { return 0; }

export { test__contract };
