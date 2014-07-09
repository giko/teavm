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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class WaitingFuture {
    private volatile Object calculatedValue;
    private volatile AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));

    public Object get() {
        CountDownLatch latch = this.latch.get();
        if (latch != null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return calculatedValue;
    }

    public void set(Object value) {
        CountDownLatch latch = this.latch.get();
        if (latch == null) {
            throw new IllegalStateException("Future already calculated");
        }
        if (!this.latch.compareAndSet(latch, null)) {
            throw new IllegalStateException("Future already calculated");
        }
        calculatedValue = value;
        latch.countDown();
    }
}
