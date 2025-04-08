package org.htmlunit.websocket.client.jetty9;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.htmlunit.websocket.client.api.WebSocketAdapter;
import org.htmlunit.websocket.client.api.WebSocketAdapterFactory;
import org.htmlunit.websocket.client.api.WebSocketListener;

/**
 * Jetty9 based impl of the WebSocketAdapter.
 * To avoid conflicts with other jetty versions used by projects, we use
 * our own shaded version of jetty9 (https://github.com/HtmlUnit/htmlunit-websocket-client).
 *
 * @author Ronald Brill
 */
public final class JettyWebSocketAdapter implements WebSocketAdapter {

    /**
     * Our {@link WebSocketAdapterFactory}.
     */
    public static final class JettyWebSocketAdapterFactory implements WebSocketAdapterFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public WebSocketAdapter buildWebSocketAdapter(final WebSocketListener listener, final CookieHandler cookieHandler, final Executor executor, boolean useInsecureSSL, int maxBinaryMessageSize, int maxBinaryMessageBufferSize, int maxTextMessageSize, int maxTextMessageBufferSize) {
            return new JettyWebSocketAdapter(listener, cookieHandler, executor, useInsecureSSL, maxBinaryMessageSize, maxBinaryMessageBufferSize, maxTextMessageSize, maxTextMessageBufferSize);
        }
    }

    private final Object clientLock_ = new Object();
    private WebSocketClient client_;
    private WebSocketListener listener_;

    private volatile Session incomingSession_;
    private Session outgoingSession_;

    /**
     * Ctor.
     */
    public JettyWebSocketAdapter(final WebSocketListener listener, final CookieHandler cookieHandler, final Executor executor, boolean useInsecureSSL, int maxBinaryMessageSize, int maxBinaryMessageBufferSize, int maxTextMessageSize, int maxTextMessageBufferSize) {
        super();

        if (useInsecureSSL) {
            client_ = new WebSocketClient(new SslContextFactory(true), null, null);
            // still use the deprecated method here to be backward compatible with older jetty versions
            // see https://github.com/HtmlUnit/htmlunit/issues/36
            // client_ = new WebSocketClient(new SslContextFactory.Client(true), null, null);
        }
        else {
            client_ = new WebSocketClient();
        }

        listener_ = listener;

        // use the same executor as the rest
        client_.setExecutor(executor);

        client_.getHttpClient().setCookieStore(new WebSocketCookieStore(cookieHandler));

        final WebSocketPolicy policy = client_.getPolicy();
        int size = maxBinaryMessageSize;
        if (size > 0) {
            policy.setMaxBinaryMessageSize(size);
        }
        size = maxBinaryMessageBufferSize;
        if (size > 0) {
            policy.setMaxBinaryMessageBufferSize(size);
        }
        size = maxTextMessageSize;
        if (size > 0) {
            policy.setMaxTextMessageSize(size);
        }
        size = maxTextMessageBufferSize;
        if (size > 0) {
            policy.setMaxTextMessageBufferSize(size);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws Exception {
        synchronized (clientLock_) {
            client_.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(final URI url) throws Exception {
        synchronized (clientLock_) {
            final Future<Session> connectFuture = client_.connect(new JettyWebSocketAdapterImpl(), url);
            client_.getExecutor().execute(() -> {
                try {
                    listener_.onWebSocketConnecting();
                    incomingSession_ = connectFuture.get();
                }
                catch (final Exception e) {
                    listener_.onWebSocketConnectError(e);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final Object content) throws IOException {
        if (content instanceof String) {
            outgoingSession_.getRemote().sendString((String) content);
        }
        else if (content instanceof ByteBuffer) {
            outgoingSession_.getRemote().sendBytes((ByteBuffer) content);
        }
        else {
            throw new IllegalStateException(
                    "Not Yet Implemented: WebSocket.send() was used to send non-string value");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeIncommingSession() {
        if (incomingSession_ != null) {
            incomingSession_.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeOutgoingSession() {
        if (outgoingSession_ != null) {
            outgoingSession_.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeClient() throws Exception {
        synchronized (clientLock_) {
            if (client_ != null) {
                client_.stop();
                client_.destroy();

                // TODO finally ?
                client_ = null;
            }
        }
    }

    private class JettyWebSocketAdapterImpl extends org.eclipse.jetty.websocket.api.WebSocketAdapter {

        /**
         * Ctor.
         */
        JettyWebSocketAdapterImpl() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onWebSocketConnect(final Session session) {
            super.onWebSocketConnect(session);
            outgoingSession_ = session;

            listener_.onWebSocketConnect();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onWebSocketClose(final int statusCode, final String reason) {
            super.onWebSocketClose(statusCode, reason);
            outgoingSession_ = null;

            listener_.onWebSocketClose(statusCode, reason);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onWebSocketText(final String message) {
            super.onWebSocketText(message);

            listener_.onWebSocketText(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onWebSocketBinary(final byte[] data, final int offset, final int length) {
            super.onWebSocketBinary(data, offset, length);

            listener_.onWebSocketBinary(data, offset, length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onWebSocketError(final Throwable cause) {
            super.onWebSocketError(cause);
            outgoingSession_ = null;

            listener_.onWebSocketError(cause);
        }
    }
}

