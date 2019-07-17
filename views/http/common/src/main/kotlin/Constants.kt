package edu.byu.uapi.server.http

/* 2xx: Success - yay! */

const val HTTP_OK = 200
const val HTTP_CREATED = 201
const val HTTP_ACCEPTED = 202
const val HTTP_NOT_AUTHORITATIVE = 203
const val HTTP_NO_CONTENT = 204
const val HTTP_RESET = 205
const val HTTP_PARTIAL = 206

/* 3XX: redirect - are you lost? */

const val HTTP_MULT_CHOICE = 300
const val HTTP_MOVED_PERM = 301
const val HTTP_MOVED_TEMP = 302
const val HTTP_SEE_OTHER = 303
const val HTTP_NOT_MODIFIED = 304
const val HTTP_USE_PROXY = 305

/* 4XX: client error - you done screwed up */

const val HTTP_BAD_REQUEST = 400
const val HTTP_UNAUTHORIZED = 401
const val HTTP_PAYMENT_REQUIRED = 402
const val HTTP_FORBIDDEN = 403
const val HTTP_NOT_FOUND = 404
const val HTTP_BAD_METHOD = 405
const val HTTP_NOT_ACCEPTABLE = 406
const val HTTP_PROXY_AUTH = 407
const val HTTP_CLIENT_TIMEOUT = 408
const val HTTP_CONFLICT = 409
const val HTTP_GONE = 410
const val HTTP_LENGTH_REQUIRED = 411
const val HTTP_PRECON_FAILED = 412
const val HTTP_ENTITY_TOO_LARGE = 413
const val HTTP_REQ_TOO_LONG = 414
const val HTTP_UNSUPPORTED_TYPE = 415

/* 5XX: server error - we done screwed up */

const val HTTP_INTERNAL_ERROR = 500
const val HTTP_NOT_IMPLEMENTED = 501
const val HTTP_BAD_GATEWAY = 502
const val HTTP_UNAVAILABLE = 503
const val HTTP_GATEWAY_TIMEOUT = 504
const val HTTP_VERSION = 505

