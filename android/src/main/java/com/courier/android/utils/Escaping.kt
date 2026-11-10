package com.courier.android.utils

/**
 * An id as a GraphQL string argument. toGraphQuery() puts the query text straight into a JSON
 * string, so the literal is escaped for GraphQL and then again for JSON. A plain id comes out
 * as \"id\", the way the queries write their quotes, and a quote or backslash in an id can't
 * break out of either.
 */
internal val String.graphQLArgument: String
    get() = graphQLString.replace("\\", "\\\\").replace("\"", "\\\"")

/**
 * A quoted GraphQL string literal
 */
internal val String.graphQLString: String
    get() {
        val escaped = StringBuilder("\"")
        for (char in this) {
            when (char) {
                '"' -> escaped.append("\\\"")
                '\\' -> escaped.append("\\\\")
                '\n' -> escaped.append("\\n")
                '\r' -> escaped.append("\\r")
                '\t' -> escaped.append("\\t")
                '\b' -> escaped.append("\\b")
                '\u000C' -> escaped.append("\\f")
                else -> if (char < ' ') {
                    escaped.append(String.format("\\u%04x", char.code))
                } else {
                    escaped.append(char)
                }
            }
        }
        return escaped.append("\"").toString()
    }

/**
 * One URL path segment. The Courier API stores path segments exactly as sent and doesn't decode
 * them, so an encoded ":" in an FCM token or "@" in an email user id became part of the stored
 * value. Only the characters that would change which url is requested are escaped.
 */
internal val String.urlPathSegment: String
    get() = this
        .replace("/", "%2F")
        .replace("?", "%3F")
        .replace("#", "%23")

/**
 * One query value. Only the characters that would end the value or the query are escaped.
 */
internal val String.urlQueryValue: String
    get() = this
        .replace("&", "%26")
        .replace("#", "%23")
