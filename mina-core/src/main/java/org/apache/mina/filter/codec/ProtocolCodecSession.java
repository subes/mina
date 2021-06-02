/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.filter.codec;

import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;

/**
 * A virtual {@link IoSession} that provides {@link ProtocolEncoderOutput}
 * and {@link ProtocolDecoderOutput}.  It is useful for unit-testing
 * codec and reusing codec for non-network-use (e.g. serialization).
 *
 * <h2>Encoding</h2>
 * <pre>
 * ProtocolCodecSession session = new ProtocolCodecSession();
 * ProtocolEncoder encoder = ...;
 * MessageX in = ...;
 *
 * encoder.encode(session, in, session.getProtocolEncoderOutput());
 *
 * IoBuffer buffer = session.getProtocolDecoderOutputQueue().poll();
 * </pre>
 *
 * <h2>Decoding</h2>
 * <pre>
 * ProtocolCodecSession session = new ProtocolCodecSession();
 * ProtocolDecoder decoder = ...;
 * IoBuffer in = ...;
 *
 * decoder.decode(session, in, session.getProtocolDecoderOutput());
 *
 * Object message = session.getProtocolDecoderOutputQueue().poll();
 * </pre>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtocolCodecSession extends DummySession {

    private final AbstractProtocolEncoderOutput encoderOutput = new AbstractProtocolEncoderOutput() {

    };

    private final AbstractProtocolDecoderOutput decoderOutput = new AbstractProtocolDecoderOutput() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            // Do nothing
        }
    };

    /**
     * Creates a new instance.
     */
    public ProtocolCodecSession() {
        // Do nothing
    }

    /**
     * @return the {@link ProtocolEncoderOutput} that buffers
     * {@link IoBuffer}s generated by {@link ProtocolEncoder}.
     */
    public ProtocolEncoderOutput getEncoderOutput() {
        return encoderOutput;
    }

    /**
     * @return the {@link Queue} of the buffered encoder output.
     */
    public Queue<Object> getEncoderOutputQueue() {
        return encoderOutput.messageQueue;
    }

    /**
     * @return the {@link ProtocolEncoderOutput} that buffers
     * messages generated by {@link ProtocolDecoder}.
     */
    public ProtocolDecoderOutput getDecoderOutput() {
        return decoderOutput;
    }

    /**
     * @return the {@link Queue} of the buffered decoder output.
     */
    public Queue<Object> getDecoderOutputQueue() {
        return decoderOutput.messageQueue;
    }
}
