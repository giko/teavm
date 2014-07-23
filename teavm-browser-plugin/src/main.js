EventTypes = {
    RECEIVE_VALUE : 0,
    RECEIVE_EXCEPTION : 1,
    RECEIVE_JAVA_CLASS_INFO : 2,
    RECEIVE_FUNCTOR_CLASS_INFO : 3,
    INVOKE_METHOD : 4,
    INSTANTIATE_CLASS : 5,
    GET_PROPERTY : 6,
    SET_PROPERTY : 7
}
ValueTypes = {
    OBJECT : 0,
    GLOBAL : 1,
    NUMBER : 2,
    INTEGER : 3,
    STRING : 4,
    BOOLEAN : 5,
    UNDEFINED : 6,
    NULL : 7,
    JAVA_OBJECT : 8,
    FUNCTOR : 9
}
PropertyFlags = {
    READABLE_PROPERTY : 1,
    WRITABLE_PROPERTY : 2
}

var messageIdGenerator = 0;
var messageQueue = new MessageQueue();
var connection = null;
var objects = {};
var objectIds = {};
var objectIdGenerator = 0;
var javaObjects = {};
var debuggee = null;
var globalObjectId = null;
var teavmId = null;
var proxyFutures = {};

chrome.browserAction.onClicked.addListener(function(tab) {
    injectContentScript(tab);
    attachDebugger(tab);
});
function connectToServer() {
    connection = new WebSocket("ws://localhost:8888/");
    connection.binaryType = "arraybuffer";
    connection.onmessage = function(event) {
        receiveMessage(event.data);
    }
}
function injectContentScript(tab) {
    chrome.tabs.executeScript(tab.id, { file: "content.js" });
}
function attachDebugger(tab) {
    debuggee = { tabId : tab.id };
    chrome.debugger.attach(debuggee, "1.0", (function(callback) {
        chrome.debugger.sendCommand(debuggee, "Debugger.enable", {}, callback);
    }).bind(null, (function(callback) {
        chrome.debugger.sendCommand(debuggee, "Runtime.evaluate", { expression : "window" }, function(response) {
            globalObjectId = response.result.objectId;
            callback();
        });
    }).bind(null, (function(callback) {
        chrome.debugger.sendCommand(debuggee, "Runtime.evaluate", { expression : "window.__TeaVM_remote__" },
                function(response) {
            teavmId = response.result.objectId;
            callback();
        });
    }).bind(null, connectToServer))));
}

chrome.debugger.onEvent.addListener(function(source, method, params) {
    switch (method) {
        case "Debugger.paused":
            onPause(source);
            break;
    }
});
function onPause(source) {
    var request = {
        objectId : teavmId,
        functionDeclaration : "__TeaVM_remote__.receiveMessage",
        arguments : [],
        returnByValue : false
    };
    chrome.debugger.sendCommand(source, "Runtime.callFunctionOn", request, function(response) {
        if (!response.result.objectId) {
            resume(source);
        } else {
            processScriptMessage(response.result, resume.bind(null, source));
        }
    });
}
function sendResponseToScript(response, callback) {
    var request = {
        objectId : teavmId,
        functionDeclaration : "__TeaVM_remote__.sendResponse",
        arguments : [response],
        returnByValue : false
    };
    chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
        callback();
    });
}
function resume(source) {
    console.log("Continue execution of script");
    chrome.debugger.sendCommand(source, "Debugger.resume", {});
}
function processScriptMessage(message, callback) {
    getScriptProperty(message, "type", function(value) {
        switch (value.value) {
            case "invoke-method":
                invokeProxyMethod(message, callback);
                break;
        }
    });
}
function invokeProxyMethod(message, callback) {
    getProxyMethodInvocation(message, function(invocation) {
        var messageId = ++messageIdGenerator;
        console.log("Sending command to invoke remote method %O.%O and put result into slot %d", invocation.instance,
                invocation.method, messageId);
        var output = new OutputStream();
        var valueWriter = new ValueWriter(output);
        output.writeByte(EventTypes.INVOKE_METHOD);
        output.writeInt(messageId);
        valueWriter.write(invocation.instance);
        valueWriter.write(invocation.method);
        output.writeByte(invocation.args.length);
        for (var i = 0; i < invocation.args.length; ++i) {
            valueWriter.write(invocation.args[i]);
        }
        var future = new Future();
        proxyFutures[messageId] = future;
        connection.send(output.getData());
        future.resolve(function(result) {
            sendResponseToScript(result, callback);
        });
    });
}
function getProxyMethodInvocation(message, callback) {
    var invocation = {};
    getScriptProperty(message, "methodName", function(value) {
        invocation.method = value;
        getScriptProperty(message, "instance", function(value) {
            invocation.instance = value;
            getScriptProperty(message, "args", function(value) {
                extractScriptArray(value, function(value) {
                    invocation.args = value;
                    callback(invocation);
                });
            })
        });
    })
}
function extractScriptArray(array, callback) {
    getScriptProperty(array, "length", function(value) {
        var result = [];
        var length = value.value;
        var extractElem = function(index) {
            getScriptProperty(array, index, function(value) {
                result.push(value);
                var next = index + 1;
                if (next == length) {
                    callback(result);
                } else {
                    extractElem(next);
                }
            });
        }
        extractElem(0);
    });
}
function getScriptProperty(instance, property, callback) {
    var request = {
        objectId : teavmId,
        functionDeclaration : "__TeaVM_remote__.getProperty",
        arguments : [instance, { value : property }],
        returnByValue : false
    };
    chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
        callback(response.result, response.wasThrown);
    });
}

function receiveMessage(data) {
    messageQueue.submit(processMessage.bind(null, data));
}
function processMessage(data, callback) {
    var input = new InputStream(new Int8Array(data));
    switch (input.readByte()) {
        case EventTypes.INVOKE_METHOD:
            receiveInvokeMethodMessage(input, callback);
            break;
        case EventTypes.GET_PROPERTY:
            receiveGetPropertyMessage(input, callback);
            break;
        case EventTypes.RECEIVE_JAVA_CLASS_INFO:
            receiveJavaClassInfo(input, false, callback);
            break;
        case EventTypes.RECEIVE_FUNCTOR_CLASS_INFO:
            receiveJavaClassInfo(input, true, callback);
            break;
        case EventTypes.RECEIVE_VALUE:
            receiveValue(input, callback);
            break;
        case EventTypes.RECEIVE_EXCEPTION:
            receiveException(input, callback);
            break;
    }
}
function receiveInvokeMethodMessage(input, callback) {
    var valueReader = new ValueReader(input);
    var args = [];
    var messageId = input.readInt();
    args.push(valueReader.read());
    args.push(valueReader.read());
    var argsCount = input.readByte();
    for (var i = 0; i < argsCount; ++i) {
        args.push(valueReader.read());
    }
    messageQueue.enter();
    Future.resolveAll(args, function(args) {
        console.log("Received command to invoke method %O.%O and put result to slot %d", args[0], args[1], messageId);
        var request = {
            objectId : teavmId,
            functionDeclaration : "__TeaVM_remote__.callMethod",
            arguments : args,
            returnByValue : false };
        chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
            sendValueBack(messageId, response);
            messageQueue.exit(callback);
        });
    });
}
function receiveGetPropertyMessage(input, callback) {
    var valueReader = new ValueReader(input);
    var messageId = input.readInt();
    var args = [valueReader.read(), valueReader.read()];
    messageQueue.enter();
    Future.resolveAll(args, function(args) {
        console.log("Received command to get property %O.%O slot %d", args[0], args[1], messageId);
        var request = {
            objectId : teavmId,
            functionDeclaration : "__TeaVM_remote__.getProperty",
            arguments : args,
            returnByValue : false };
        chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
            sendValueBack(messageId, response);
            messageQueue.exit(callback);
        });
    });
}
function receiveJavaClassInfo(input, functor, callback) {
    var description = { properties : {}, methods : [] };
    var classId = input.readInt();
    var propertyCount = input.readShort();
    for (var i = 0; i < propertyCount; ++i) {
        var property = input.readUTF8();
        var flags = input.readByte();
        var propertyDesc = {};
        propertyDesc.readable = (flags & PropertyFlags.READABLE_PROPERTY) != 0;
        propertyDesc.writable = (flags & PropertyFlags.WRITABLE_PROPERTY) != 0;
        description.properties[property] = propertyDesc;
    }
    var methodCount = input.readShort();
    for (var i = 0; i < methodCount; ++i) {
        var method = input.readUTF8();
        description.methods.push(method);
    }
    description.functor = functor;
    console.log("Received class proxy description #%d: %O", classId, description);
    var request = {
        objectId : teavmId,
        functionDeclaration : "__TeaVM_remote__.declareProxyClass",
        arguments : [ { value : classId }, { value : JSON.stringify(description) }]
    };
    chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
        callback();
    });
}
function receiveValue(input, callback) {
    var valueReader = new ValueReader(input);
    var messageId = input.readInt();
    var value = valueReader.readValue();
    var future = proxyFutures[messageId];
    delete proxyFutures[messageId];
    value.resolve(function(result) {
        console.log("Received value %O to slot #%d", result, messageId);
        future.set(result);
        callback();
    });
}
function receiveException(input, callback) {
    var valueReader = new ValueReader(input);
    var messageId = input.readInt();
    var exception = input.readUTF8();
    delete proxyFutures[messageId];
    console.log("Received exception %s to slot #%d", exception, messageId);
    future.error(exception);
    callback();
}
function sendValueBack(messageId, response) {
    var out = new OutputStream();
    var valueWriter = new ValueWriter(out);
    if (!response.wasThrown) {
        out.writeByte(EventTypes.RECEIVE_VALUE);
        out.writeInt(messageId);
        valueWriter.write(response.result);
        console.log("Sending value %O to slot #%d", response.result, messageId);
    } else {
        out.writeByte(EventTypes.RECEIVE_EXCEPTION);
        out.writeInt(messageId);
        out.writeUTF8(response.result.description);
        console.log("Sending exception %O to slot #%d", response.result.description, messageId);
    }
    connection.send(out.getData());
}

function ValueReader(input) {
    this.input = input;
}
ValueReader.prototype.read = function() {
    switch (this.input.readByte()) {
        case ValueTypes.BOOLEAN:
            return new Future({ value : this.input.readByte() == 1 });
        case ValueTypes.GLOBAL:
            return new Future({ objectId : globalObjectId });
        case ValueTypes.INTEGER:
            return new Future({ value : this.input.readInt() });
        case ValueTypes.JAVA_OBJECT:
            return this.readJavaObject();
        case ValueTypes.NULL:
            return new Future({ value : null });
        case ValueTypes.NUMBER:
            return new Future({ value : this.input.readNumber() });
        case ValueTypes.OBJECT:
            return new Future({ objectId : objects[this.input.readInt()] });
        case ValueTypes.STRING:
            return new Future({ value : this.input.readUTF8() });
        case ValueTypes.UNDEFINED:
            return new Future({ value : undefined });
        
    }
}
ValueReader.prototype.readJavaObject = function() {
    var future = new Future();
    var id = this.input.readInt();
    var classId = this.input.readInt();
    var object = javaObjects[id];
    if (object === undefined) {
        var request = {
            objectId : teavmId,
            functionDeclaration : "__TeaVM_remote__.createProxy",
            arguments : [ { value : classId }, { value : id }]
        };
        chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
            javaObjects[id] = response.result;
            objectIds[response.result.objectId] = { type : "Java", id : id, classId : classId };
            future.set(response.result);
        });
    } else {
        future.set(object);
    }
    return future;
}

function ValueWriter(output) {
    this.output = output;
}
ValueWriter.prototype.write = function(value) {
    switch (value.type) {
        case "boolean":
            this.output.writeByte(ValueTypes.BOOLEAN);
            this.output.writeByte(value.value ? 1 : 0);
            break;
        case "number":
            if (value.value == (value.value | 0)) {
                this.output.writeByte(ValueTypes.INTEGER);
                this.output.writeInt(value.value);
            } else {
                this.output.writeByte(ValueTypes.NUMBER);
                this.output.writeNumber(value.value);
            }
            break;
        case "undefined":
            this.output.writeByte(ValueTypes.UNDEFINED);
            break;
        case "string":
            this.output.writeByte(ValueTypes.STRING);
            this.output.writeUTF8(value.value);
            break;
        case "object":
        case "function":
            if (value.objectId) {
                this.writeObject(value.objectId);
            } else {
                this.output.writeByte(ValueTypes.NULL);
            }
            break;
    }
}
ValueWriter.prototype.writeObject = function(objectId) {
    if (objectId == globalObjectId) {
        this.output.writeByte(ValueTypes.GLOBAL);
    } else {
        var localId = objectIds[objectId];
        if (localId === undefined) {
            localId = { type : "JS", id : ++objectIdGenerator };
            objectIds[objectId] = localId;
            objects[localId.id] = objectId;
        }
        switch (localId.type) {
            case "JS":
                this.output.writeByte(ValueTypes.OBJECT)
                this.output.writeInt(localId.id);
                break;
            case "Java":
                this.output.writeByte(ValueTypes.JAVA_OBJECT)
                this.output.writeInt(localId.id);
                this.output.writeInt(localId.classId);
                break;
            case "Functor":
                this.output.writeByte(ValueTypes.FUNCTOR)
                this.output.writeInt(localId.id);
                this.output.writeInt(localId.classId);
                break;
        }
    }
}