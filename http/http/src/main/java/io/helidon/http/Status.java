/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

import java.util.Objects;

/**
 * Commonly used status codes defined by HTTP, see
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10">HTTP/1.1 documentation</a>.
 * Additional status codes can be added by applications
 * by call {@link #create(int)} or {@link #create(int, String)} with unknown status code, or with text
 * that differs from the predefined status codes.
 * <p>
 * Although the constants are instances of this class, they can be compared using instance equality, as the only
 * way to obtain an instance is through methods {@link #create(int)} {@link #create(int, String)}, which ensures
 * the same instance is returned for known status codes and reason phrases.
 * <p>
 * A good reference is the IANA list of HTTP Status Codes (we may not cover all of them in this type):
 * <a href="https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml">IANA HTTP Status Codes</a>
 * <p>
 * For each status, there is also a constant with the status code (int), to allow usage in annotations.
 */
public class Status {
    /**
     * 100 Continue,
     * see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.1">HTTP/1.1 documentations</a>.
     */
    public static final int CONTINUE_100_INT = 100;
    /**
     * 100 Continue,
     * see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.1">HTTP/1.1 documentations</a>.
     */
    public static final Status CONTINUE_100 = new Status(CONTINUE_100_INT, "Continue", true);
    /**
     * 101 Switching Protocols,
     * see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.2">HTTP/1.1 documentations</a>.
     */
    public static final int SWITCHING_PROTOCOLS_101_INT = 101;
    /**
     * 101 Switching Protocols,
     * see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.2">HTTP/1.1 documentations</a>.
     */
    public static final Status SWITCHING_PROTOCOLS_101 = new Status(SWITCHING_PROTOCOLS_101_INT, "Switching Protocols", true);

    /**
     * 200 OK, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.1">HTTP/1.1 documentation</a>.
     */
    public static final int OK_200_INT = 200;
    /**
     * 200 OK, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.1">HTTP/1.1 documentation</a>.
     */
    public static final Status OK_200 = new Status(OK_200_INT, "OK", true);

    /**
     * 201 Created, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.2">HTTP/1.1 documentation</a>.
     */
    public static final int CREATED_201_INT = 201;
    /**
     * 201 Created, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.2">HTTP/1.1 documentation</a>.
     */
    public static final Status CREATED_201 = new Status(CREATED_201_INT, "Created", true);

    /**
     * 202 Accepted, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.3">HTTP/1.1 documentation</a>
     */
    public static final int ACCEPTED_202_INT = 202;
    /**
     * 202 Accepted, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.3">HTTP/1.1 documentation</a>
     */
    public static final Status ACCEPTED_202 = new Status(ACCEPTED_202_INT, "Accepted", true);

    /**
     * 203 Non-Authoritative Information, see
     * <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.4">HTTP/1.1 documentation</a>.
     *
     * @since 4.0.6
     */
    public static final int NON_AUTHORITATIVE_INFORMATION_203_INT = 203;
    /**
     * 203 Non-Authoritative Information, see
     * <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.4">HTTP/1.1 documentation</a>.
     *
     * @since 4.0.6
     */
    public static final Status NON_AUTHORITATIVE_INFORMATION_203 = new Status(NON_AUTHORITATIVE_INFORMATION_203_INT,
                                                                              "Non-Authoritative Information",
                                                                              true);

    /**
     * 204 No Content, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.5">HTTP/1.1 documentation</a>.
     */
    public static final int NO_CONTENT_204_INT = 204;
    /**
     * 204 No Content, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.5">HTTP/1.1 documentation</a>.
     */
    public static final Status NO_CONTENT_204 = new Status(NO_CONTENT_204_INT, "No Content", true);

    /**
     * 205 Reset Content, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.6">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int RESET_CONTENT_205_INT = 205;
    /**
     * 205 Reset Content, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.6">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status RESET_CONTENT_205 = new Status(RESET_CONTENT_205_INT, "Reset Content", true);

    /**
     * 206 Reset Content, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.7">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int PARTIAL_CONTENT_206_INT = 206;
    /**
     * 206 Reset Content, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.7">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status PARTIAL_CONTENT_206 = new Status(PARTIAL_CONTENT_206_INT, "Partial Content", true);

    /**
     * 207 Multi-Status, see
     * <a href="https://www.rfc-editor.org/rfc/rfc4918.html#section-13">RFC 4918 - HTTP Extensions for WebDAV</a>.
     *
     * @since 4.0.6
     */
    public static final int MULTI_STATUS_207_INT = 207;
    /**
     * 207 Multi-Status, see
     * <a href="https://www.rfc-editor.org/rfc/rfc4918.html#section-13">RFC 4918 - HTTP Extensions for WebDAV</a>.
     *
     * @since 4.0.6
     */
    public static final Status MULTI_STATUS_207 = new Status(MULTI_STATUS_207_INT, "Multi-Status", true);

    /**
     * 301 Moved Permanently, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.2">HTTP/1.1 documentation</a>.
     */
    public static final int MOVED_PERMANENTLY_301_INT = 301;
    /**
     * 301 Moved Permanently, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.2">HTTP/1.1 documentation</a>.
     */
    public static final Status MOVED_PERMANENTLY_301 = new Status(MOVED_PERMANENTLY_301_INT, "Moved Permanently", true);

    /**
     * 302 Found, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.3">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int FOUND_302_INT = 302;
    /**
     * 302 Found, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.3">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status FOUND_302 = new Status(FOUND_302_INT, "Found", true);

    /**
     * 303 See Other, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.4">HTTP/1.1 documentation</a>.
     */
    public static final int SEE_OTHER_303_INT = 303;
    /**
     * 303 See Other, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.4">HTTP/1.1 documentation</a>.
     */
    public static final Status SEE_OTHER_303 = new Status(SEE_OTHER_303_INT, "See Other", true);

    /**
     * 304 Not Modified, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5">HTTP/1.1 documentation</a>.
     */
    public static final int NOT_MODIFIED_304_INT = 304;
    /**
     * 304 Not Modified, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5">HTTP/1.1 documentation</a>.
     */
    public static final Status NOT_MODIFIED_304 = new Status(NOT_MODIFIED_304_INT, "Not Modified", true);

    /**
     * 305 Use Proxy, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.6">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int USE_PROXY_305_INT = 305;
    /**
     * 305 Use Proxy, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.6">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status USE_PROXY_305 = new Status(USE_PROXY_305_INT, "Use Proxy", true);

    /**
     * 307 Temporary Redirect, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.8">HTTP/1.1 documentation</a>.
     */
    public static final int TEMPORARY_REDIRECT_307_INT = 307;
    /**
     * 307 Temporary Redirect, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.8">HTTP/1.1 documentation</a>.
     */
    public static final Status TEMPORARY_REDIRECT_307 = new Status(TEMPORARY_REDIRECT_307_INT, "Temporary Redirect", true);

    /**
     * 308 Permanent Redirect, see
     * <a href="https://www.rfc-editor.org/rfc/rfc7538">HTTP Status Code 308 documentation</a>.
     */
    public static final int PERMANENT_REDIRECT_308_INT = 308;
    /**
     * 308 Permanent Redirect, see
     * <a href="https://www.rfc-editor.org/rfc/rfc7538">HTTP Status Code 308 documentation</a>.
     */
    public static final Status PERMANENT_REDIRECT_308 = new Status(PERMANENT_REDIRECT_308_INT, "Permanent Redirect", true);

    /**
     * 400 Bad Request, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">HTTP/1.1 documentation</a>.
     */
    public static final int BAD_REQUEST_400_INT = 400;
    /**
     * 400 Bad Request, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">HTTP/1.1 documentation</a>.
     */
    public static final Status BAD_REQUEST_400 = new Status(BAD_REQUEST_400_INT, "Bad Request", true);

    /**
     * 401 Unauthorized, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2">HTTP/1.1 documentation</a>.
     */
    public static final int UNAUTHORIZED_401_INT = 401;
    /**
     * 401 Unauthorized, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2">HTTP/1.1 documentation</a>.
     */
    public static final Status UNAUTHORIZED_401 = new Status(UNAUTHORIZED_401_INT, "Unauthorized", true);

    /**
     * 402 Payment Required, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.3">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int PAYMENT_REQUIRED_402_INT = 402;
    /**
     * 402 Payment Required, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.3">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status PAYMENT_REQUIRED_402 = new Status(PAYMENT_REQUIRED_402_INT, "Payment Required", true);

    /**
     * 403 Forbidden, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4">HTTP/1.1 documentation</a>.
     */
    public static final int FORBIDDEN_403_INT = 403;
    /**
     * 403 Forbidden, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4">HTTP/1.1 documentation</a>.
     */
    public static final Status FORBIDDEN_403 = new Status(FORBIDDEN_403_INT, "Forbidden", true);

    /**
     * 404 Not Found, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">HTTP/1.1 documentation</a>.
     */
    public static final int NOT_FOUND_404_INT = 404;
    /**
     * 404 Not Found, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">HTTP/1.1 documentation</a>.
     */
    public static final Status NOT_FOUND_404 = new Status(NOT_FOUND_404_INT, "Not Found", true);

    /**
     * 405 Method Not Allowed, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.6">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int METHOD_NOT_ALLOWED_405_INT = 405;
    /**
     * 405 Method Not Allowed, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.6">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status METHOD_NOT_ALLOWED_405 = new Status(METHOD_NOT_ALLOWED_405_INT, "Method Not Allowed", true);

    /**
     * 406 Not Acceptable, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.7">HTTP/1.1 documentation</a>.
     */
    public static final int NOT_ACCEPTABLE_406_INT = 406;
    /**
     * 406 Not Acceptable, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.7">HTTP/1.1 documentation</a>.
     */
    public static final Status NOT_ACCEPTABLE_406 = new Status(NOT_ACCEPTABLE_406_INT, "Not Acceptable", true);

    /**
     * 407 Proxy Authentication Required, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.8">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int PROXY_AUTHENTICATION_REQUIRED_407_INT = 407;
    /**
     * 407 Proxy Authentication Required, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.8">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status PROXY_AUTHENTICATION_REQUIRED_407 = new Status(PROXY_AUTHENTICATION_REQUIRED_407_INT,
                                                                              "Proxy Authentication Required",
                                                                              true);

    /**
     * 408 Request Timeout, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.9">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int REQUEST_TIMEOUT_408_INT = 408;
    /**
     * 408 Request Timeout, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.9">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status REQUEST_TIMEOUT_408 = new Status(REQUEST_TIMEOUT_408_INT, "Request Timeout", true);

    /**
     * 409 Conflict, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.10">HTTP/1.1 documentation</a>.
     */
    public static final int CONFLICT_409_INT = 409;
    /**
     * 409 Conflict, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.10">HTTP/1.1 documentation</a>.
     */
    public static final Status CONFLICT_409 = new Status(CONFLICT_409_INT, "Conflict", true);

    /**
     * 410 Gone, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.11">HTTP/1.1 documentation</a>.
     */
    public static final int GONE_410_INT = 410;
    /**
     * 410 Gone, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.11">HTTP/1.1 documentation</a>.
     */
    public static final Status GONE_410 = new Status(GONE_410_INT, "Gone", true);

    /**
     * 411 Length Required, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.12">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int LENGTH_REQUIRED_411_INT = 411;
    /**
     * 411 Length Required, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.12">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status LENGTH_REQUIRED_411 = new Status(LENGTH_REQUIRED_411_INT, "Length Required", true);

    /**
     * 412 Precondition Failed, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.13">HTTP/1.1 documentation</a>.
     */
    public static final int PRECONDITION_FAILED_412_INT = 412;
    /**
     * 412 Precondition Failed, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.13">HTTP/1.1 documentation</a>.
     */
    public static final Status PRECONDITION_FAILED_412 = new Status(PRECONDITION_FAILED_412_INT, "Precondition Failed", true);

    /**
     * 413 Request Entity Too Large, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.14">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int REQUEST_ENTITY_TOO_LARGE_413_INT = 413;
    /**
     * 413 Request Entity Too Large, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.14">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status REQUEST_ENTITY_TOO_LARGE_413 = new Status(REQUEST_ENTITY_TOO_LARGE_413_INT,
                                                                         "Request Entity Too Large",
                                                                         true);

    /**
     * 414 Request-URI Too Long, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.15">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int REQUEST_URI_TOO_LONG_414_INT = 414;
    /**
     * 414 Request-URI Too Long, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.15">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status REQUEST_URI_TOO_LONG_414 = new Status(REQUEST_URI_TOO_LONG_414_INT, "Request-URI Too Long", true);

    /**
     * 415 Unsupported Media Type, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.16">HTTP/1.1 documentation</a>.
     */
    public static final int UNSUPPORTED_MEDIA_TYPE_415_INT = 415;
    /**
     * 415 Unsupported Media Type, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.16">HTTP/1.1 documentation</a>.
     */
    public static final Status UNSUPPORTED_MEDIA_TYPE_415 = new Status(UNSUPPORTED_MEDIA_TYPE_415_INT,
                                                                       "Unsupported Media Type",
                                                                       true);

    /**
     * 416 Requested Range Not Satisfiable, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.17">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int REQUESTED_RANGE_NOT_SATISFIABLE_416_INT = 416;
    /**
     * 416 Requested Range Not Satisfiable, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.17">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status REQUESTED_RANGE_NOT_SATISFIABLE_416 = new Status(REQUESTED_RANGE_NOT_SATISFIABLE_416_INT,
                                                                                "Requested Range Not Satisfiable",
                                                                                true);

    /**
     * 417 Expectation Failed, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.18">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int EXPECTATION_FAILED_417_INT = 417;
    /**
     * 417 Expectation Failed, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.18">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status EXPECTATION_FAILED_417 = new Status(EXPECTATION_FAILED_417_INT, "Expectation Failed", true);

    /**
     * 418 I'm a teapot, see
     * <a href="https://tools.ietf.org/html/rfc2324#section-2.3.2">Hyper Text Coffee Pot Control Protocol (HTCPCP/1.0)</a>.
     */
    public static final int I_AM_A_TEAPOT_418_INT = 418;
    /**
     * 418 I'm a teapot, see
     * <a href="https://tools.ietf.org/html/rfc2324#section-2.3.2">Hyper Text Coffee Pot Control Protocol (HTCPCP/1.0)</a>.
     */
    public static final Status I_AM_A_TEAPOT_418 = new Status(I_AM_A_TEAPOT_418_INT, "I'm a teapot", true);

    /**
     * Misdirected request, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-421-misdirected-request">RFC 9110 - Http Semantics</a>.
     */
    public static final int MISDIRECTED_REQUEST_421_INT = 421;
    /**
     * Misdirected request, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-421-misdirected-request">RFC 9110 - Http Semantics</a>.
     */
    public static final Status MISDIRECTED_REQUEST_421 = new Status(MISDIRECTED_REQUEST_421_INT, "Misdirected Request", true);

    /**
     * Unprocessable content, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-422-unprocessable-content">RFC 9110 - Http Semantics</a>.
     */
    public static final int UNPROCESSABLE_CONTENT_422_INT = 422;
    /**
     * Unprocessable content, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-422-unprocessable-content">RFC 9110 - Http Semantics</a>.
     */
    public static final Status UNPROCESSABLE_CONTENT_422 = new Status(UNPROCESSABLE_CONTENT_422_INT,
                                                                      "Unprocessable Content",
                                                                      true);

    /**
     * Locked, see
     * <a href="https://www.rfc-editor.org/rfc/rfc4918.html#page-78">RFC 4918 - HTTP Extensions for WebDAV</a>.
     */
    public static final int LOCKED_423_INT = 423;
    /**
     * Locked, see
     * <a href="https://www.rfc-editor.org/rfc/rfc4918.html#page-78">RFC 4918 - HTTP Extensions for WebDAV</a>.
     */
    public static final Status LOCKED_423 = new Status(LOCKED_423_INT, "Locked", true);

    /**
     * Failed dependency, see
     * <a href="https://www.rfc-editor.org/rfc/rfc4918.html#section-11.4">RFC 4918 - HTTP Extensions for WebDAV</a>.
     */
    public static final int FAILED_DEPENDENCY_424_INT = 424;
    /**
     * Failed dependency, see
     * <a href="https://www.rfc-editor.org/rfc/rfc4918.html#section-11.4">RFC 4918 - HTTP Extensions for WebDAV</a>.
     */
    public static final Status FAILED_DEPENDENCY_424 = new Status(FAILED_DEPENDENCY_424_INT, "Failed Dependency", true);

    /**
     * Upgrade required, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-426-upgrade-required">RFC 9110 - Http Semantics</a>.
     */
    public static final int UPGRADE_REQUIRED_426_INT = 426;
    /**
     * Upgrade required, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-426-upgrade-required">RFC 9110 - Http Semantics</a>.
     */
    public static final Status UPGRADE_REQUIRED_426 = new Status(UPGRADE_REQUIRED_426_INT, "Upgrade Required", true);

    /**
     * Precondition required, see
     * <a href="https://www.rfc-editor.org/rfc/rfc6585.html#page-2">RFC 6585 - Additional HTTP Status Codes</a>.
     */
    public static final int PRECONDITION_REQUIRED_428_INT = 428;
    /**
     * Precondition required, see
     * <a href="https://www.rfc-editor.org/rfc/rfc6585.html#page-2">RFC 6585 - Additional HTTP Status Codes</a>.
     */
    public static final Status PRECONDITION_REQUIRED_428 = new Status(PRECONDITION_REQUIRED_428_INT,
                                                                      "Precondition Required",
                                                                      true);

    /**
     * Too many requests, see
     * <a href="https://www.rfc-editor.org/rfc/rfc6585.html#page-3">RFC 6585 - Additional HTTP Status Codes</a>.
     */
    public static final int TOO_MANY_REQUESTS_429_INT = 429;
    /**
     * Too many requests, see
     * <a href="https://www.rfc-editor.org/rfc/rfc6585.html#page-3">RFC 6585 - Additional HTTP Status Codes</a>.
     */
    public static final Status TOO_MANY_REQUESTS_429 = new Status(TOO_MANY_REQUESTS_429_INT, "Too Many Requests", true);

    /**
     * 500 Internal Server Error, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.1">HTTP/1.1 documentation</a>.
     */
    public static final int INTERNAL_SERVER_ERROR_500_INT = 500;
    /**
     * 500 Internal Server Error, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.1">HTTP/1.1 documentation</a>.
     */
    public static final Status INTERNAL_SERVER_ERROR_500 = new Status(INTERNAL_SERVER_ERROR_500_INT,
                                                                      "Internal Server Error",
                                                                      true);

    /**
     * 501 Not Implemented, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.2">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int NOT_IMPLEMENTED_501_INT = 501;
    /**
     * 501 Not Implemented, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.2">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status NOT_IMPLEMENTED_501 = new Status(NOT_IMPLEMENTED_501_INT, "Not Implemented", true);

    /**
     * 502 Bad Gateway, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.3">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int BAD_GATEWAY_502_INT = 502;
    /**
     * 502 Bad Gateway, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.3">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status BAD_GATEWAY_502 = new Status(BAD_GATEWAY_502_INT, "Bad Gateway", true);

    /**
     * 503 Service Unavailable, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.4">HTTP/1.1 documentation</a>.
     */
    public static final int SERVICE_UNAVAILABLE_503_INT = 503;
    /**
     * 503 Service Unavailable, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.4">HTTP/1.1 documentation</a>.
     */
    public static final Status SERVICE_UNAVAILABLE_503 = new Status(SERVICE_UNAVAILABLE_503_INT, "Service Unavailable", true);

    /**
     * 504 Gateway Timeout, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.5">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final int GATEWAY_TIMEOUT_504_INT = 504;
    /**
     * 504 Gateway Timeout, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.5">HTTP/1.1 documentation</a>.
     *
     * @since 2.0
     */
    public static final Status GATEWAY_TIMEOUT_504 = new Status(GATEWAY_TIMEOUT_504_INT, "Gateway Timeout", true);

    /**
     * 505 HTTP Version Not Supported, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.6">HTTP/1.1 documentation</a>.
     *
     * @since 3.0.3
     */
    public static final int HTTP_VERSION_NOT_SUPPORTED_505_INT = 505;
    /**
     * 505 HTTP Version Not Supported, see
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.6">HTTP/1.1 documentation</a>.
     *
     * @since 3.0.3
     */
    public static final Status HTTP_VERSION_NOT_SUPPORTED_505 = new Status(HTTP_VERSION_NOT_SUPPORTED_505_INT,
                                                                           "HTTP Version Not Supported",
                                                                           true);

    /**
     * 507 Insufficient Storage, see
     * <a href="http://www.ietf.org/rfc/rfc4918.txt">WebDAV documentation</a>.
     *
     * @since 4.0.6
     */
    public static final int INSUFFICIENT_STORAGE_507_INT = 507;
    /**
     * 507 Insufficient Storage, see
     * <a href="http://www.ietf.org/rfc/rfc4918.txt">WebDAV documentation</a>.
     *
     * @since 4.0.6
     */
    public static final Status INSUFFICIENT_STORAGE_507 = new Status(INSUFFICIENT_STORAGE_507_INT, "Insufficient Storage", true);

    /**
     * 508 Loop Detected, see
     * <a href="https://www.rfc-editor.org/rfc/rfc5842#section-7.2">RFC 5842 - Bindings for the Constrained Application Protocol
     * (CoAP)</a>.
     *
     * @since 4.0.6
     */
    public static final int LOOP_DETECTED_508_INT = 508;
    /**
     * 508 Loop Detected, see
     * <a href="https://www.rfc-editor.org/rfc/rfc5842#section-7.2">RFC 5842 - Bindings for the Constrained Application Protocol
     * (CoAP)</a>.
     *
     * @since 4.0.6
     */
    public static final Status LOOP_DETECTED_508 = new Status(LOOP_DETECTED_508_INT, "Loop Detected", true);

    /**
     * 510 Not Extended, see
     * <a href="https://www.rfc-editor.org/rfc/rfc2774#section-7">RFC 2774 - An HTTP Extension Framework</a>.
     *
     * @since 4.0.6
     */
    public static final int NOT_EXTENDED_510_INT = 510;
    /**
     * 510 Not Extended, see
     * <a href="https://www.rfc-editor.org/rfc/rfc2774#section-7">RFC 2774 - An HTTP Extension Framework</a>.
     *
     * @since 4.0.6
     */
    public static final Status NOT_EXTENDED_510 = new Status(NOT_EXTENDED_510_INT, "Not Extended", true);

    /**
     * 511 Network Authentication Required, see
     * <a href="https://www.rfc-editor.org/rfc/rfc6585#section-6">RFC 6585 - Additional HTTP Status Codes</a>.
     *
     * @since 4.0.6
     */
    public static final int NETWORK_AUTHENTICATION_REQUIRED_511_INT = 511;
    /**
     * 511 Network Authentication Required, see
     * <a href="https://www.rfc-editor.org/rfc/rfc6585#section-6">RFC 6585 - Additional HTTP Status Codes</a>.
     *
     * @since 4.0.6
     */
    public static final Status NETWORK_AUTHENTICATION_REQUIRED_511 = new Status(NETWORK_AUTHENTICATION_REQUIRED_511_INT,
                                                                                "Network Authentication Required",
                                                                                true);

    static {
        // THIS MUST BE AFTER THE LAST CONSTANT
        StatusHelper.statusesDone();
    }

    private final int code;
    private final String reason;
    private final Family family;
    private final String codeText;
    private final String stringValue;

    private Status(int statusCode, String reasonPhrase, boolean instance) {
        this.code = statusCode;
        this.reason = reasonPhrase;
        this.family = Family.of(statusCode);
        this.codeText = String.valueOf(code);
        this.stringValue = code + " " + reason;

        if (instance) {
            StatusHelper.add(this);
        }
    }

    private Status(int statusCode, String reasonPhrase, Family family, String codeText) {
        // for custom codes
        this.code = statusCode;
        this.reason = reasonPhrase;
        this.family = family;
        this.codeText = codeText;
        this.stringValue = code + " " + reason;
    }

    /**
     * Convert a numerical status code into the corresponding Status.
     * <p>
     * For an unknown code, an ad-hoc {@link Status} is created.
     *
     * @param statusCode the numerical status code
     * @return the matching Status; either a constant from this class, or an ad-hoc {@link Status}
     */
    public static Status create(int statusCode) {
        Status found = StatusHelper.find(statusCode);

        if (found == null) {
            return createNew(Family.of(statusCode), statusCode, "", String.valueOf(statusCode));
        }
        return found;
    }

    /**
     * Convert a numerical status code into the corresponding Status.
     * <p>
     * It either returns an existing {@link Status} constant if possible.
     * For an unknown code, or code/reason phrase combination it creates
     * an ad-hoc {@link Status}.
     *
     * @param statusCode   the numerical status code
     * @param reasonPhrase the reason phrase; if {@code null} or a known reason phrase, an instance with the default
     *                     phrase is returned; otherwise, a new instance is returned
     * @return the matching Status
     */
    public static Status create(int statusCode, String reasonPhrase) {
        Status found = StatusHelper.find(statusCode);
        if (found == null) {
            return createNew(Family.of(statusCode), statusCode, reasonPhrase, String.valueOf(statusCode));
        }
        if (reasonPhrase == null) {
            return found;
        }
        if (found.reasonPhrase().equalsIgnoreCase(reasonPhrase)) {
            return found;
        }
        return createNew(found.family(), statusCode, reasonPhrase, found.codeText());
    }

    /**
     * Get the associated integer value representing the status code.
     *
     * @return the integer value representing the status code.
     */
    public int code() {
        return code;
    }

    /**
     * Get the class of status code.
     *
     * @return the class of status code.
     */
    public Family family() {
        return family;
    }

    /**
     * Get the reason phrase.
     *
     * @return the reason phrase.
     */
    public String reasonPhrase() {
        return reason;
    }

    /**
     * Text of the {@link #code()}.
     *
     * @return code string (number as a string)
     */
    public String codeText() {
        return codeText;
    }

    /**
     * Get the response status as string.
     *
     * @return the response status string in the form of a partial HTTP response status line,
     *         i.e. {@code "Status-Code SP Reason-Phrase"}.
     */
    public String toString() {
        return stringValue;
    }

    /**
     * Text of the status as used in HTTP/1, such as "200 OK".
     *
     * @return text of this status
     */
    public String text() {
        return stringValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Status status = (Status) o;
        return code == status.code && reason.equals(status.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, reason);
    }

    private static Status createNew(Family family, int statusCode, String reasonPhrase, String codeText) {
        return new Status(statusCode, reasonPhrase, family, codeText);
    }

    /**
     * An enumeration representing the class of status code. Family is used
     * here since class is overloaded in Java.
     * <p>
     * Copied from JAX-RS.
     */
    public enum Family {

        /**
         * {@code 1xx} HTTP status codes.
         */
        INFORMATIONAL,
        /**
         * {@code 2xx} HTTP status codes.
         */
        SUCCESSFUL,
        /**
         * {@code 3xx} HTTP status codes.
         */
        REDIRECTION,
        /**
         * {@code 4xx} HTTP status codes.
         */
        CLIENT_ERROR,
        /**
         * {@code 5xx} HTTP status codes.
         */
        SERVER_ERROR,
        /**
         * Other, unrecognized HTTP status codes.
         */
        OTHER;

        /**
         * Get the family for the response status code.
         *
         * @param statusCode response status code to get the family for.
         * @return family of the response status code.
         */
        public static Family of(int statusCode) {
            return switch (statusCode / 100) {
                case 1 -> Family.INFORMATIONAL;
                case 2 -> Family.SUCCESSFUL;
                case 3 -> Family.REDIRECTION;
                case 4 -> Family.CLIENT_ERROR;
                case 5 -> Family.SERVER_ERROR;
                default -> Family.OTHER;
            };
        }
    }
}
