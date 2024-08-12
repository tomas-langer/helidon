/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.http;

import java.util.HashMap;
import java.util.Map;

import io.helidon.common.buffers.Ascii;

/*
 * Do not add random headers here. These headers are optimized for performance, and each header added to this enum
 * will slightly increase the memory used by each HTTP request.
 */
enum HeaderNameEnum implements HeaderName {
    ACCEPT(Strings.ACCEPT_STRING),
    ACCEPT_CHARSET(Strings.ACCEPT_CHARSET_STRING),
    ACCEPT_ENCODING(Strings.ACCEPT_ENCODING_STRING),
    ACCEPT_LANGUAGE(Strings.ACCEPT_LANGUAGE_STRING),
    ACCEPT_DATETIME(Strings.ACCEPT_DATETIME_STRING),
    ACCESS_CONTROL_ALLOW_CREDENTIALS(Strings.ACCESS_CONTROL_ALLOW_CREDENTIALS_STRING),
    ACCESS_CONTROL_ALLOW_HEADERS(Strings.ACCESS_CONTROL_ALLOW_HEADERS_STRING),
    ACCESS_CONTROL_ALLOW_METHODS(Strings.ACCESS_CONTROL_ALLOW_METHODS_STRING),
    ACCESS_CONTROL_ALLOW_ORIGIN(Strings.ACCESS_CONTROL_ALLOW_ORIGIN_STRING),
    ACCESS_CONTROL_EXPOSE_HEADERS(Strings.ACCESS_CONTROL_EXPOSE_HEADERS_STRING),
    ACCESS_CONTROL_MAX_AGE(Strings.ACCESS_CONTROL_MAX_AGE_STRING),
    ACCESS_CONTROL_REQUEST_HEADERS(Strings.ACCESS_CONTROL_REQUEST_HEADERS_STRING),
    ACCESS_CONTROL_REQUEST_METHOD(Strings.ACCESS_CONTROL_REQUEST_METHOD_STRING),
    AUTHORIZATION(Strings.AUTHORIZATION_STRING),
    COOKIE(Strings.COOKIE_STRING),
    EXPECT(Strings.EXPECT_STRING),
    FORWARDED(Strings.FORWARDED_STRING),
    FROM(Strings.FROM_STRING),
    HOST(Strings.HOST_STRING),
    IF_MATCH(Strings.IF_MATCH_STRING),
    IF_MODIFIED_SINCE(Strings.IF_MODIFIED_SINCE_STRING),
    IF_NONE_MATCH(Strings.IF_NONE_MATCH_STRING),
    IF_RANGE(Strings.IF_RANGE_STRING),
    IF_UNMODIFIED_SINCE(Strings.IF_UNMODIFIED_SINCE_STRING),
    MAX_FORWARDS(Strings.MAX_FORWARDS_STRING),
    ORIGIN(Strings.ORIGIN_STRING),
    PROXY_AUTHENTICATE(Strings.PROXY_AUTHENTICATE_STRING),
    PROXY_AUTHORIZATION(Strings.PROXY_AUTHORIZATION_STRING),
    RANGE(Strings.RANGE_STRING),
    REFERER(Strings.REFERER_STRING),
    REFRESH(Strings.REFRESH_STRING),
    TE(Strings.TE_STRING),
    USER_AGENT(Strings.USER_AGENT_STRING),
    VIA(Strings.VIA_STRING),
    ACCEPT_PATCH(Strings.ACCEPT_PATCH_STRING),
    ACCEPT_RANGES(Strings.ACCEPT_RANGES_STRING),
    AGE(Strings.AGE_STRING),
    ALLOW(Strings.ALLOW_STRING),
    ALT_SVC(Strings.ALT_SVC_STRING),
    CACHE_CONTROL(Strings.CACHE_CONTROL_STRING),
    CONNECTION(Strings.CONNECTION_STRING),
    CONTENT_DISPOSITION(Strings.CONTENT_DISPOSITION_STRING),
    CONTENT_ENCODING(Strings.CONTENT_ENCODING_STRING),
    CONTENT_LANGUAGE(Strings.CONTENT_LANGUAGE_STRING),
    CONTENT_LENGTH(Strings.CONTENT_LENGTH_STRING),
    CONTENT_LOCATION(Strings.CONTENT_LOCATION_STRING),
    CONTENT_RANGE(Strings.CONTENT_RANGE_STRING),
    CONTENT_TYPE(Strings.CONTENT_TYPE_STRING),
    DATE(Strings.DATE_STRING),
    ETAG(Strings.ETAG_STRING),
    EXPIRES(Strings.EXPIRES_STRING),
    LAST_MODIFIED(Strings.LAST_MODIFIED_STRING),
    LINK(Strings.LINK_STRING),
    LOCATION(Strings.LOCATION_STRING),
    PRAGMA(Strings.PRAGMA_STRING),
    PUBLIC_KEY_PINS(Strings.PUBLIC_KEY_PINS_STRING),
    RETRY_AFTER(Strings.RETRY_AFTER_STRING),
    SERVER(Strings.SERVER_STRING),
    SET_COOKIE(Strings.SET_COOKIE_STRING),
    SET_COOKIE2(Strings.SET_COOKIE2_STRING),
    STRICT_TRANSPORT_SECURITY(Strings.STRICT_TRANSPORT_SECURITY_STRING),
    TRAILER(Strings.TRAILER_STRING),
    TRANSFER_ENCODING(Strings.TRANSFER_ENCODING_STRING),
    TSV(Strings.TSV_STRING),
    UPGRADE(Strings.UPGRADE_STRING),
    VARY(Strings.VARY_STRING),
    WARNING(Strings.WARNING_STRING),
    WWW_AUTHENTICATE(Strings.WWW_AUTHENTICATE_STRING),
    X_FORWARDED_FOR(Strings.X_FORWARDED_FOR_STRING),
    X_FORWARDED_HOST(Strings.X_FORWARDED_HOST_STRING),
    X_FORWARDED_PORT(Strings.X_FORWARDED_PORT_STRING),
    X_FORWARDED_PREFIX(Strings.X_FORWARDED_PREFIX_STRING),
    X_FORWARDED_PROTO(Strings.X_FORWARDED_PROTO_STRING),
    X_HELIDON_CN(Strings.X_HELIDON_CN_STRING);

    private static final Map<String, HeaderName> BY_NAME;
    private static final Map<String, HeaderName> BY_CAP_NAME;

    static {
        Map<String, HeaderName> byName = new HashMap<>();
        Map<String, HeaderName> byCapName = new HashMap<>();
        for (HeaderNameEnum value : HeaderNameEnum.values()) {
            byName.put(value.lowerCase(), value);
            byCapName.put(value.defaultCase(), value);
        }
        BY_NAME = byName;
        BY_CAP_NAME = byCapName;
    }

    private final String lowerCase;
    private final String http1Case;
    private final int index;

    HeaderNameEnum(String http1Case) {
        this.http1Case = http1Case;
        this.lowerCase = this.http1Case.toLowerCase();
        this.index = this.ordinal();
    }

    static HeaderName byCapitalizedName(String name) {
        HeaderName found = BY_CAP_NAME.get(name);
        if (found == null) {
            return byName(Ascii.toLowerCase(name));
        }
        return found;
    }

    static HeaderName byName(String lowerCase) {
        return BY_NAME.get(lowerCase);
    }

    @Override
    public String lowerCase() {
        return lowerCase;
    }

    @Override
    public String defaultCase() {
        return http1Case;
    }

    @Override
    public int index() {
        return index;
    }

    static class Strings {
        static final String ACCEPT_STRING = "Accept";
        static final String ACCEPT_CHARSET_STRING = "Accept-Charset";
        static final String ACCEPT_ENCODING_STRING = "Accept-Encoding";
        static final String ACCEPT_LANGUAGE_STRING = "Accept-Language";
        static final String ACCEPT_DATETIME_STRING = "Accept-Datetime";
        static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_STRING = "Access-Control-Allow-Credentials";
        static final String ACCESS_CONTROL_ALLOW_HEADERS_STRING = "Access-Control-Allow-Headers";
        static final String ACCESS_CONTROL_ALLOW_METHODS_STRING = "Access-Control-Allow-Methods";
        static final String ACCESS_CONTROL_ALLOW_ORIGIN_STRING = "Access-Control-Allow-Origin";
        static final String ACCESS_CONTROL_EXPOSE_HEADERS_STRING = "Access-Control-Expose-Headers";
        static final String ACCESS_CONTROL_MAX_AGE_STRING = "Access-Control-Max-Age";
        static final String ACCESS_CONTROL_REQUEST_HEADERS_STRING = "Access-Control-Request-Headers";
        static final String ACCESS_CONTROL_REQUEST_METHOD_STRING = "Access-Control-Request-Method";
        static final String AUTHORIZATION_STRING = "Authorization";
        static final String COOKIE_STRING = "Cookie";
        static final String EXPECT_STRING = "Expect";
        static final String FORWARDED_STRING = "Forwarded";
        static final String FROM_STRING = "From";
        static final String HOST_STRING = "Host";
        static final String IF_MATCH_STRING = "If-Match";
        static final String IF_MODIFIED_SINCE_STRING = "If-Modified-Since";
        static final String IF_NONE_MATCH_STRING = "If-None-Match";
        static final String IF_RANGE_STRING = "If-Range";
        static final String IF_UNMODIFIED_SINCE_STRING = "If-Unmodified-Since";
        static final String MAX_FORWARDS_STRING = "Max-Forwards";
        static final String ORIGIN_STRING = "Origin";
        static final String PROXY_AUTHENTICATE_STRING = "Proxy-Authenticate";
        static final String PROXY_AUTHORIZATION_STRING = "Proxy-Authorization";
        static final String RANGE_STRING = "Range";
        static final String REFERER_STRING = "Referer";
        static final String REFRESH_STRING = "Refresh";
        static final String TE_STRING = "TE";
        static final String USER_AGENT_STRING = "User-Agent";
        static final String VIA_STRING = "Via";
        static final String ACCEPT_PATCH_STRING = "Accept-Patch";
        static final String ACCEPT_RANGES_STRING = "Accept-Ranges";
        static final String AGE_STRING = "Age";
        static final String ALLOW_STRING = "Allow";
        static final String ALT_SVC_STRING = "Alt-Svc";
        static final String CACHE_CONTROL_STRING = "Cache-Control";
        static final String CONNECTION_STRING = "Connection";
        static final String CONTENT_DISPOSITION_STRING = "Content-Disposition";
        static final String CONTENT_ENCODING_STRING = "Content-Encoding";
        static final String CONTENT_LANGUAGE_STRING = "Content-Language";
        static final String CONTENT_LENGTH_STRING = "Content-Length";
        static final String CONTENT_LOCATION_STRING = "aa";
        static final String CONTENT_RANGE_STRING = "Content-Range";
        static final String CONTENT_TYPE_STRING = "Content-Type";
        static final String DATE_STRING = "Date";
        static final String ETAG_STRING = "ETag";
        static final String EXPIRES_STRING = "Expires";
        static final String LAST_MODIFIED_STRING = "Last-Modified";
        static final String LINK_STRING = "Link";
        static final String LOCATION_STRING = "Location";
        static final String PRAGMA_STRING = "Pragma";
        static final String PUBLIC_KEY_PINS_STRING = "Public-Key-Pins";
        static final String RETRY_AFTER_STRING = "Retry-After";
        static final String SERVER_STRING = "Server";
        static final String SET_COOKIE_STRING = "Set-Cookie";
        static final String SET_COOKIE2_STRING = "Set-Cookie2";
        static final String STRICT_TRANSPORT_SECURITY_STRING = "Strict-Transport-Security";
        static final String TRAILER_STRING = "Trailer";
        static final String TRANSFER_ENCODING_STRING = "Transfer-Encoding";
        static final String TSV_STRING = "TSV";
        static final String UPGRADE_STRING = "Upgrade";
        static final String VARY_STRING = "Vary";
        static final String WARNING_STRING = "Warning";
        static final String WWW_AUTHENTICATE_STRING = "WWW-Authenticate";
        static final String X_FORWARDED_FOR_STRING = "X-Forwarded-For";
        static final String X_FORWARDED_HOST_STRING = "X-Forwarded-Host";
        static final String X_FORWARDED_PORT_STRING = "X-Forwarded-Port";
        static final String X_FORWARDED_PREFIX_STRING = "X-Forwarded-Prefix";
        static final String X_FORWARDED_PROTO_STRING = "X-Forwarded-Proto";
        static final String X_HELIDON_CN_STRING = "X-HELIDON-CN";

        private Strings() {
        }
    }
}
