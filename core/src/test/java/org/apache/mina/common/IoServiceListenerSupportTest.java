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
package org.apache.mina.common;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.easymock.MockControl;

/**
 * Tests {@link IoServiceListenerSupport}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceListenerSupportTest extends TestCase {
    private static final SocketAddress ADDRESS = new InetSocketAddress(8080);

    private final IoService mockService = MockControl
            .createControl(IoService.class).getMock();

    public void testServiceLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport(
                mockService);

        MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Test activation
        listener.serviceActivated(mockService);

        listenerControl.replay();

        support.add(listener);
        support.fireServiceActivated();

        listenerControl.verify();

        // Test deactivation & other side effects
        listenerControl.reset();
        listener.serviceDeactivated(mockService);

        listenerControl.replay();
        //// Activate more than once
        support.fireServiceActivated();
        //// Deactivate
        support.fireServiceDeactivated();
        //// Deactivate more than once
        support.fireServiceDeactivated();

        listenerControl.verify();
    }

    public void testSessionLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport(
                mockService);

        DummySession session = new DummySession();
        session.setService(mockService);
        session.setLocalAddress(ADDRESS);

        MockControl handlerControl = MockControl
                .createStrictControl(IoHandler.class);
        IoHandler handler = (IoHandler) handlerControl.getMock();
        session.setHandler(handler);

        MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Test creation
        listener.sessionCreated(session);
        handler.sessionCreated(session);
        handler.sessionOpened(session);

        listenerControl.replay();
        handlerControl.replay();

        support.add(listener);
        support.fireSessionCreated(session);

        listenerControl.verify();
        handlerControl.verify();

        Assert.assertEquals(1, support.getManagedSessions().size());
        Assert.assertTrue(support.getManagedSessions().contains(session));

        // Test destruction & other side effects
        listenerControl.reset();
        handlerControl.reset();
        handler.sessionClosed(session);
        listener.sessionDestroyed(session);

        listenerControl.replay();
        //// Activate more than once
        support.fireSessionCreated(session);
        //// Deactivate
        support.fireSessionDestroyed(session);
        //// Deactivate more than once
        support.fireSessionDestroyed(session);

        listenerControl.verify();

        Assert.assertTrue(session.isClosing());
        Assert.assertEquals(0, support.getManagedSessions().size());
        Assert.assertFalse(support.getManagedSessions().contains(session));
    }

    public void testDisconnectOnUnbind() throws Exception {
        MockControl acceptorControl = MockControl
                .createStrictControl(IoAcceptor.class);
        IoAcceptor acceptor = (IoAcceptor) acceptorControl.getMock();

        final IoServiceListenerSupport support = new IoServiceListenerSupport(
                acceptor);

        final DummySession session = new DummySession();
        session.setService(acceptor);
        session.setLocalAddress(ADDRESS);

        MockControl handlerControl = MockControl
                .createStrictControl(IoHandler.class);
        IoHandler handler = (IoHandler) handlerControl.getMock();
        session.setHandler(handler);

        final MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Activate a service and create a session.
        listener.serviceActivated(acceptor);
        listener.sessionCreated(session);
        handler.sessionCreated(session);
        handler.sessionOpened(session);

        listenerControl.replay();
        handlerControl.replay();

        support.add(listener);
        support.fireServiceActivated();
        support.fireSessionCreated(session);

        listenerControl.verify();
        handlerControl.verify();

        // Deactivate a service and make sure the session is closed & destroyed.
        listenerControl.reset();
        handlerControl.reset();

        listener.serviceDeactivated(acceptor);
        acceptorControl.expectAndReturn(acceptor.isCloseOnDeactivation(), true);
        listener.sessionDestroyed(session);
        handler.sessionClosed(session);

        listenerControl.replay();
        acceptorControl.replay();
        handlerControl.replay();

        new Thread() {
            // Emulate I/O service
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // This synchronization block is a workaround for
                // the visibility problem of simultaneous EasyMock
                // state update. (not sure if it fixes the failing test yet.)
                synchronized (listenerControl) {
                    support.fireSessionDestroyed(session);
                }
            }
        }.start();
        support.fireServiceDeactivated();

        synchronized (listenerControl) {
            listenerControl.verify();
        }
        acceptorControl.verify();
        handlerControl.verify();

        Assert.assertTrue(session.isClosing());
        Assert.assertEquals(0, support.getManagedSessions().size());
        Assert.assertFalse(support.getManagedSessions().contains(session));
    }

    public void testConnectorActivation() throws Exception {
        MockControl connectorControl = MockControl
                .createStrictControl(IoConnector.class);
        IoConnector connector = (IoConnector) connectorControl.getMock();

        IoServiceListenerSupport support = new IoServiceListenerSupport(
                connector);

        final DummySession session = new DummySession();
        session.setService(connector);
        session.setRemoteAddress(ADDRESS);

        MockControl handlerControl = MockControl
                .createStrictControl(IoHandler.class);
        IoHandler handler = (IoHandler) handlerControl.getMock();
        session.setHandler(handler);

        MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Creating a session should activate a service automatically.
        listener.serviceActivated(connector);
        listener.sessionCreated(session);
        handler.sessionCreated(session);
        handler.sessionOpened(session);

        listenerControl.replay();
        handlerControl.replay();

        support.add(listener);
        support.fireSessionCreated(session);

        listenerControl.verify();
        handlerControl.verify();

        // Destroying a session should deactivate a service automatically.
        listenerControl.reset();
        handlerControl.reset();
        listener.sessionDestroyed(session);
        handler.sessionClosed(session);
        listener.serviceDeactivated(connector);

        listenerControl.replay();
        handlerControl.replay();

        support.fireSessionDestroyed(session);

        listenerControl.verify();
        handlerControl.verify();

        Assert.assertEquals(0, support.getManagedSessions().size());
        Assert.assertFalse(support.getManagedSessions().contains(session));
    }
}
