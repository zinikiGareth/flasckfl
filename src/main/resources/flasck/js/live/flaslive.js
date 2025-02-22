// src/main/javascript/live/jsenv.js
import { CommonEnv } from "/js/flasjs.js";
import { SimpleBroker } from "/js/ziwsh.js";
var JSEnv = function(broker) {
  if (broker == null)
    broker = new SimpleBroker(console, this, {});
  var logger = {
    log: console.log,
    debugmsg: console.log
  };
  CommonEnv.call(this, logger, broker);
};
JSEnv.prototype = new CommonEnv();
JSEnv.prototype.constructor = JSEnv;
JSEnv.prototype.addHistory = function(state, title, url) {
  history.pushState(state, title, url);
};
JSEnv.prototype.replaceRoute = function(url) {
  history.replaceState(history.state, null, url);
};
export {
  JSEnv
};
