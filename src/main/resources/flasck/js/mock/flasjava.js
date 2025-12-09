// src/main/javascript/forjava/wsbridge.js
import { UTRunner } from "/js/flastest.js";
function WSBridge(host, port) {
  var self = this;
  this.unittests = {};
  this.systemtests = {};
  this.runner = new UTRunner(this);
  this.currentTest = null;
  this.ws = new WebSocket("ws://" + host + ":" + port + "/bridge");
  this.requestId = 1;
  this.sending = [];
  this.lockedOut = [];
  this.responders = {};
  this.moduleCreators = {};
  this.ws.addEventListener("open", (ev) => {
    console.log("wsbridge connected");
    while (this.sending.length > 0) {
      var v = this.sending.shift();
      console.log("bridge =>", v);
      this.ws.send(v);
    }
  });
  this.ws.addEventListener("message", (ev) => {
    console.log("bridge <=", ev.data);
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
  console.log("adding unit test", name);
  this.unittests[name] = test;
};
WSBridge.prototype.addSystemTest = function(name, test) {
  console.log("adding system test", name);
  this.systemtests[name] = test;
};
WSBridge.prototype.error = function(...args) {
  console.log.apply(console.log, args);
  if (this.ws) {
    this.send({ action: "error", error: merge(args) });
  }
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
var merge = function(args) {
  var ret = "";
  var sep = "";
  for (var i = 0; i < args.length; i++) {
    ret += sep + args[i];
    sep = " ";
  }
  return ret;
};
WSBridge.prototype.connectModule = function(moduleName, callback) {
  console.log("creating module", moduleName);
  var reqId = this.requestId++;
  this.moduleCreators[moduleName] = { reqId, callback };
  this.lock(reqId, "bindModule");
  this.send({ action: "module", "name": moduleName });
  return "must-wait";
};
WSBridge.handlers["haveModule"] = function(msg) {
  console.log("have module", msg.name);
  var name = msg.name;
  var cbr = this.moduleCreators[name];
  if (cbr && cbr.callback) {
    cbr.callback(this.runner, msg.conn);
  }
  delete this.moduleCreators[name];
  this.unlock(cbr.reqId, "haveModule");
};
WSBridge.handlers["prepareUnitTest"] = function(msg) {
  console.log("UNIT", msg.wrapper, msg.testname);
  var cxt = this.runner.newContext();
  var utf = this.unittests[msg.wrapper][msg.testname];
  this.currentTest = new utf(this.runner, cxt);
  this.runner.clear();
  var steps = this.currentTest.dotest.call(this.currentTest, cxt);
  this.send({ action: "steps", steps });
};
WSBridge.handlers["prepareSystemTest"] = function(msg) {
  console.log("SYSTEST", msg.testclz);
  var cxt = this.runner.newContext();
  var stc = this.systemtests[msg.testclz];
  this.currentTest = new stc(this.runner, cxt);
  this.runner.clear();
  this.send({ action: "systemTestPrepared" });
};
WSBridge.handlers["prepareStage"] = function(msg) {
  console.log("PREPARE STAGE", msg.stage);
  var cxt = this.runner.newContext();
  var stage = this.currentTest[msg.stage];
  var steps = stage(cxt);
  this.send({ action: "steps", steps });
};
WSBridge.handlers["runStep"] = function(msg) {
  console.log("RUN STEP", msg.step);
  try {
    var cxt = this.runner.newContext();
    var step = this.currentTest[msg.step];
    step.call(this.currentTest, cxt);
  } catch (e) {
    console.log(e);
    this.error(e.toString());
  }
  this.unlock(msg.reqId, "runstep");
};
WSBridge.handlers["assertSatisfied"] = function(msg) {
  console.log("assert all expectations satisfied", msg);
  try {
    this.runner.assertSatisfied();
    this.runner.checkAtEnd();
  } catch (e) {
    console.log(e);
    this.error(e.toString());
  }
  this.unlock(msg.reqId, "assertSatisfied");
};
WSBridge.prototype.send = function(json) {
  var text = JSON.stringify(json);
  if (this.ws.readyState == this.ws.OPEN) {
    if (json.action != "log") {
      console.log("bridge =>", text);
    }
    this.ws.send(text);
  } else
    this.sending.push(text);
};
WSBridge.prototype.executeSync = function(runner, st, cxt, steps) {
  this.runner = runner;
  this.st = st;
  this.runcxt = cxt;
  this.readysteps = steps;
  this.unlock("ready to go");
};
WSBridge.prototype.nextRequestId = function(hdlr) {
  this.responders[this.requestId] = hdlr;
  return this.requestId++;
};
WSBridge.prototype.lock = function(reqId, msg) {
  console.log("lock", msg);
  this.send({ action: "lock", msg, reqId });
};
WSBridge.prototype.unlock = function(reqId, msg) {
  console.log("unlock", msg);
  this.send({ action: "unlock", msg, reqId });
};
export {
  WSBridge
};
