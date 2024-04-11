// src/main/javascript/forjava/wsbridge.js
import { UTRunner } from "/js/flastest.js";
function WSBridge(host2, port2) {
  var self = this;
  this.unittests = {};
  this.systemtests = {};
  this.runner = new UTRunner(this);
  this.currentTest = null;
  this.ws = new WebSocket("ws://" + host2 + ":" + port2 + "/bridge");
  this.requestId = 1;
  this.sending = [];
  this.lockedOut = [];
  this.responders = {};
  this.moduleCreators = {};
  this.ws.addEventListener("open", (ev) => {
    console.log("connected", ev);
    while (this.sending.length > 0) {
      var v = this.sending.shift();
      this.ws.send(v);
    }
  });
  this.ws.addEventListener("message", (ev) => {
    console.log("message", ev.data);
    var msg = JSON.parse(ev.data);
    var action = msg.action;
    if (action == "response") {
      var rid = msg.respondingTo;
      if (!this.responders[rid]) {
        console.log("there is nobody willing to handle response " + rid);
        return;
      }
      this.responders[rid].call(this, msg);
      delete this.responders[rid];
    } else {
      if (!WSBridge.handlers[action]) {
        console.log("there is no handler for " + action);
        return;
      }
      WSBridge.handlers[action].call(self, msg);
    }
  });
}
WSBridge.handlers = {};
WSBridge.prototype.addUnitTest = function(name, test) {
  console.log("adding unit test", name, test);
  this.unittests[name] = test;
};
WSBridge.prototype.addSystemTest = function(name, test) {
  console.log("adding system test", name, test);
  this.systemtests[name] = test;
};
WSBridge.prototype.log = function(...args) {
  console.log.apply(console.log, args);
  if (this.ws) {
    this.send({ action: "log", message: merge(args) });
  }
};
WSBridge.prototype.debugmsg = function(...args) {
  console.log.apply(console.log, args);
  this.send({ action: "debugmsg", message: merge(args) });
};
var merge = function(...args) {
  var ret = "";
  var sep = "";
  for (var i = 0; i < arguments.length; i++) {
    ret += sep + arguments[i];
    sep = " ";
  }
  return ret;
};
WSBridge.prototype.module = function(moduleName, callback) {
  console.log("creating module", moduleName);
  this.moduleCreators[moduleName] = callback;
  this.lock("bindModule");
  this.send({ action: "module", "name": moduleName });
  return "must-wait";
};
WSBridge.handlers["haveModule"] = function(msg) {
  console.log("have module", msg.name);
  var name = msg.name;
  var cb = this.moduleCreators[name];
  if (cb) {
    cb(this.runner, msg.conn);
  }
  delete this.moduleCreators[name];
  this.unlock("haveModule");
};
WSBridge.handlers["prepareUnitTest"] = function(msg) {
  console.log("run unit test", msg);
  var cxt = this.runner.newContext();
  var utf = this.unittests[msg.wrapper][msg.testname];
  this.currentTest = new utf(this.runner, cxt);
  this.runner.clear();
  var steps = this.currentTest.dotest.call(this.currentTest, cxt);
  this.send({ action: "steps", steps });
};
WSBridge.handlers["prepareSystemTest"] = function(msg) {
  console.log("run system test", msg);
  var cxt = this.runner.newContext();
  console.log("test", this.systemtests[msg.testclz]);
  var stc = this.systemtests[msg.testclz];
  console.log("what is", stc);
  this.currentTest = new stc(this.runner, cxt);
  this.runner.clear();
  this.send({ action: "systemTestPrepared" });
};
WSBridge.handlers["prepareStage"] = function(msg) {
  console.log("prepare stage", msg);
  var cxt = this.runner.newContext();
  var stage = this.currentTest[msg.stage];
  var steps = stage(cxt);
  this.send({ action: "steps", steps });
};
WSBridge.handlers["runStep"] = function(msg) {
  console.log("run unit test step", msg);
  try {
    var cxt = this.runner.newContext();
    var step = this.currentTest[msg.step];
    step.call(this.currentTest, cxt);
    this.unlock("runstep");
  } catch (e) {
    console.log(e);
    this.send({ action: "error", error: e.toString() });
  }
};
WSBridge.handlers["assertSatisfied"] = function(msg) {
  console.log("assert all expectations satisfied", msg);
  try {
    this.runner.assertSatisfied();
    this.runner.checkAtEnd();
    this.unlock("assertSatisfied");
  } catch (e) {
    console.log(e);
    this.send({ action: "error", error: e.toString() });
  }
};
WSBridge.prototype.send = function(json) {
  var text = JSON.stringify(json);
  if (this.ws.readyState == this.ws.OPEN)
    this.ws.send(text);
  else
    this.sending.push(text);
};
WSBridge.prototype.connectToZiniki = function(wsapi, cb) {
  runner.broker.connectToServer("ws://" + host + ":" + port);
};
WSBridge.prototype.executeSync = function(runner2, st, cxt, steps) {
  this.runner = runner2;
  this.st = st;
  this.runcxt = cxt;
  this.readysteps = steps;
  this.unlock("ready to go");
};
WSBridge.prototype.nextRequestId = function(hdlr) {
  this.responders[this.requestId] = hdlr;
  return this.requestId++;
};
WSBridge.prototype.lock = function(msg) {
  console.log("lock", msg);
  this.send({ action: "lock", msg });
};
WSBridge.prototype.unlock = function(msg) {
  console.log("unlock", msg);
  this.send({ action: "unlock", msg });
};
export {
  WSBridge
};
