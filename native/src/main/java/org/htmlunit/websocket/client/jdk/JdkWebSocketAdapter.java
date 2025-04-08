package org.htmlunit.websocket.client.jdk;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.htmlunit.websocket.client.api.WebSocketAdapter;
import org.htmlunit.websocket.client.api.WebSocketAdapterFactory;
import org.htmlunit.websocket.client.api.WebSocketListener;

public final class JdkWebSocketAdapter implements WebSocketAdapter {

    /**
     * Our {@link WebSocketAdapterFactory}.
     */
    public static final class JdkWebSocketAdapterFactory implements WebSocketAdapterFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public WebSocketAdapter buildWebSocketAdapter(final WebSocketListener listener, final CookieHandler cookieHandler, final Executor executor, boolean useInsecureSSL, int maxBinaryMessageSize, int maxBinaryMessageBufferSize, int maxTextMessageSize, int maxTextMessageBufferSize) {
            return new JdkWebSocketAdapter(listener, cookieHandler, executor, useInsecureSSL, maxBinaryMessageSize, maxBinaryMessageBufferSize, maxTextMessageSize, maxTextMessageBufferSize);
        }
    }

    private final WebSocket.Builder clientBuilder;
    private final WebSocketListener listener;

    private WebSocket socket;

    public JdkWebSocketAdapter(final WebSocketListener listener, final CookieHandler cookieHandler, final Executor executor, boolean useInsecureSSL, int maxBinaryMessageSize, int maxBinaryMessageBufferSize, int maxTextMessageSize, int maxTextMessageBufferSize) {
        super();
        if (useInsecureSSL) {
            throw new IllegalArgumentException("JdkWebSocketAdapter does not support insecure SSL/TLS");
        }
        var httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieHandler)
                .executor(executor)
                .build();


        this.listener = listener;
        clientBuilder = httpClient.newWebSocketBuilder();


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws Exception {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(final URI url) throws Exception {
        clientBuilder.buildAsync(url, new JdkWebSocketAdapterImpl());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final Object content) throws IOException {


        if (content instanceof String s) {
            socket.sendText(s,false);
        }
        else if (content instanceof ByteBuffer b) {
            socket.sendBinary(b, false);
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
        if (socket != null && !socket.isInputClosed()) {
            socket.abort();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeOutgoingSession() {
        if (socket != null && !socket.isOutputClosed()) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE,"NORMAL_CLOSURE");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeClient() throws Exception {

    }

    private class JdkWebSocketAdapterImpl implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
            listener.onWebSocketConnecting();
            socket = webSocket;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            listener.onWebSocketText(data.toString());
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            listener.onWebSocketBinary(data.array(), data.position(), data.remaining());

            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            socket = null;
            listener.onWebSocketClose(statusCode, reason);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            WebSocket.Listener.super.onError(webSocket, error);
            socket = null;
            listener.onWebSocketConnectError(error);
        }

    }
}

