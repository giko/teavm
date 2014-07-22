/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.jso.devmode;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class WaitingFuture {
    private volatile Object calculatedValue;
    private volatile Exception exception;
    private AtomicBoolean calculated = new AtomicBoolean();
    private EventQueue eventQueue;

    public WaitingFuture(EventQueue eventQueue) {
        this.eventQueue = eventQueue;
    }

    public Object get() {
        while (!calculated.get()) {
            eventQueue.exec();
        }
        if (exception != null) {
            throw new JSRemoteException("JavaScript exception occured", exception);
        }
        return calculatedValue;
    }

    public void set(Object value) {
        if (!calculated.compareAndSet(false, true)) {
            throw new IllegalStateException("Future already calculated");
        }
        calculatedValue = value;
        eventQueue.stop();
    }

    public void setException(Exception e) {
        if (!calculated.compareAndSet(false, true)) {
            throw new IllegalStateException("Future already calculated");
        }
        exception = e;
        eventQueue.stop();
    }
}
