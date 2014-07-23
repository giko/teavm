/**
 * Allows to run asynchronous operations one after another
 */
function MessageQueue() {
    this.taskQueue = [];
    this.processing = false;
    this.stack = [];
    this.stopCallback = null;
}
/**
 * Submits a task that is to perform after another pending tasks.
 * @param task a function to submit. This function should take callback and then call it when the task is performed.
 */
MessageQueue.prototype.submit = function(task) {
    this.taskQueue.push(task);
    if (!this.processing) {
        this.processing = true;
        this.process((function() {
            this.processing = false;
            if (this.stopCallback) {
                var callback = this.stopCallback;
                this.stopCallback = null;
                callback();
            }
        }).bind(this));
    }
}
MessageQueue.prototype.enter = function() {
    this.stack.push({ queue : this.taskQueue, wasProcessing : this.processing });
    this.taskQueue = [];
    this.processing = false;
    this.stopCallback = null;
}
MessageQueue.prototype.exit = function(callback) {
    if (this.stopCallback) {
        throw new Error("Previous message queue is running");
    }
    this.stopCallback = (function() {
        this.stopCallback = null;
        var record = this.stack.pop();
        this.taskQueue = record.queue;
        this.processing = record.wasProcessing;
        callback();
    }).bind(this);
    if (!this.processing) {
        this.stopCallback();
    }
}
MessageQueue.prototype.process = function(callback) {
    var task = this.taskQueue.shift();
    task(function() {
        if (this.taskQueue.length == 0) {
            callback();
        } else {
            this.process(callback);
        }
    }.bind(this));
}

function InputStream(data) {
    this.data = data;
    this.index = 0;
}
InputStream.prototype.readByte = function() {
    return this.data[this.index++];
}
InputStream.prototype.readShort = function() {
    var a = this.data[this.index++];
    var b = this.data[this.index++];
    var r = ((a & 0xFF) << 8) | (b & 0xFF);
    return (r << 16) >> 16;
}
InputStream.prototype.readInt = function() {
    var a = this.data[this.index++];
    var b = this.data[this.index++];
    var c = this.data[this.index++];
    var d = this.data[this.index++];
    return ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((c & 0xFF) << 8) | (d & 0xFF);
}
InputStream.prototype.readNumber = function() {
    var data = new Float64Array(this.data, this.index, 8);
    var value = data[0];
    this.index += 8;
    return value;
}
InputStream.prototype.readUTF8 = function() {
    var length = this.readShort();
    var str = "";
    for (var i = 0; i < length; ++i) {
        var code;
        var b = this.readByte();
        if ((b & 0x80) == 0x00) {
            code = b;
        } else if ((b & 0xE0) == 0xC0) {
            code = ((b & 0x1F) << 6) | (this.readByte() & 0x3F);
        } else if ((b & 0xF0) == 0xE0) {
            code = ((b & 0x1F) << 12) | ((this.readByte() & 0x3F) << 6) | (this.readByte() & 0x3F);
        }
        str += String.fromCharCode(code);
    }
    return str;
};

function OutputStream() {
    this.buffer = new ArrayBuffer(64);
    this.data = new Int8Array(this.buffer);
    this.index = 0;
}
OutputStream.prototype.alloc = function(size) {
    if (this.index + size >= this.data.length) {
        var newSize = Math.max((this.index + size) * 2, this.data.length);
        this.buffer = new ArrayBuffer(newSize);
        var newData = new Int8Array(this.buffer);
        for (var i = 0; i < index; ++i) {
            newData[i] = this.data[i];
        }
        this.data = newData;
    }
}
OutputStream.prototype.writeByte = function(value) {
    this.alloc(1);
    this.data[this.index++] = value;
}
OutputStream.prototype.writeShort = function(value) {
    this.alloc(2);
    this.data[this.index++] = (value >> 8) & 0xFF;
    this.data[this.index++] = (value >> 0) & 0xFF;
}
OutputStream.prototype.writeInt = function(value) {
    this.alloc(4);
    this.data[this.index++] = (value >> 24) & 0xFF;
    this.data[this.index++] = (value >> 16) & 0xFF;
    this.data[this.index++] = (value >> 8) & 0xFF;
    this.data[this.index++] = (value >> 0) & 0xFF;
}
OutputStream.prototype.writeNumber = function(value) {
    this.alloc(8);
    var data = new Float64Array(this.buffer, this.index, 8);
    data[0] = value;
    this.index += 8;
}
OutputStream.prototype.writeUTF8 = function(value) {
    var length = 2;
    for (var i = 0; i < value.length; ++i) {
        var c = value.charCodeAt(i);
        if (c < 0x0080) {
            length += 1;
        } else if (c < 0x0800) {
            length += 2;
        } else {
            length += 3;
        }
    }
    this.alloc(length);
    this.data[this.index++] = (value.length >> 8) & 0xFF;
    this.data[this.index++] = (value.length >> 0) & 0xFF;
    for (var i = 0; i < value.length; ++i) {
        var c = value.charCodeAt(i);
        if (c < 0x0080) {
            this.data[this.index++] = c & 0x7F;
        } else if (c < 0x0800) {
            this.data[this.index++] = 0xC0 | ((c >> 6) & 0x1F);
            this.data[this.index++] = 0x80 | (c & 0x3F);
        } else {
            this.data[this.index++] = 0xC0 | ((c >> 12) & 0x1F);
            this.data[this.index++] = 0x80 | ((c >> 6) & 0x3F);
            this.data[this.index++] = 0x80 | (c & 0x3F);
        }
    }
}
OutputStream.prototype.getData = function() {
    var resultBuffer = new ArrayBuffer(this.index);
    var resultArray = new Int8Array(resultBuffer);
    for (var i = 0; i < this.index; ++i) {
        resultArray[i] = this.data[i];
    }
    return resultBuffer;
}

function Future(value) {
    if (value === undefined) {
        this.value = null;
        this.error = null;
        this.calculated = false;
        this.callbacks = [];
    } else {
        this.value = value;
        this.error = null;
        this.calculated = true;
        this.callbacks = null;
    }
}
Future.prototype.resolve = function(callback, error) {
    if (this.calculated) {
        if (!this.error) {
            callback(this.value);
        } else {
            error(this.error);
        }
    } else {
        this.callbacks.push({ callback : callback, error : error });
    }
}
Future.prototype.set = function(value) {
    if (this.calculated) {
        throw new Error("Value already calculated");
    }
    this.value = value;
    this.calculated = true;
    for (var i = 0; i < this.callbacks.length; ++i) {
        this.callbacks[i].callback(value);
    }
    this.callbacks = null;
}
Future.prototype.error = function(error) {
    if (this.calculated) {
        throw new Error("Value already calculated");
    }
    this.error = error;
    this.calculated = true;
    for (var i = 0; i < this.callbacks.length; ++i) {
        var errorHandler = this.callbacks[i].error;
        if (errorHandler) {
            errorHandler(error);
        }
    }
    this.callbacks = null;
}
Future.resolveAll = function(futures, callback, error) {
    var left = futures.length;
    var values = [];
    var stopped = false;
    for (var i = 0; i < futures.length; ++i) {
        values.push(null);
        futures[i].resolve((function(index) {
            return function(value) {
                if (stopped) {
                    return;
                }
                values[index] = value;
                if (--left == 0) {
                    callback(values);
                }
            };
        })(i), function(err) {
            if (stopped) {
                return;
            }
            stopped = true;
            error(err);
        });
    }
}