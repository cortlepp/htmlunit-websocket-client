/*
 * Copyright (c) 2002-2025 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.htmlunit.websocket.client.jetty9;

import java.net.CookieHandler;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A helper class for {@link WebSocket}.
 *
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
class WebSocketCookieStore implements CookieStore {

    private final CookieHandler cookieHandler;

    WebSocketCookieStore(final CookieHandler cookieHandler) {
        this.cookieHandler = cookieHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final URI uri, final HttpCookie cookie) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HttpCookie> get(final URI uri) {
        try {
            final String urlString = uri.toString().replace("ws://", "http://").replace("wss://", "https://");
            final URI finalUri = URI.create(urlString);
            final List<HttpCookie> cookies = cookieHandler.get(finalUri,null).entrySet().stream().map(e -> new HttpCookie(e.getKey(), e.getValue().get(0))).collect(Collectors.toList());
            return cookies;
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HttpCookie> getCookies() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<URI> getURIs() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final URI uri, final HttpCookie cookie) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll() {
        return false;
    }
}
