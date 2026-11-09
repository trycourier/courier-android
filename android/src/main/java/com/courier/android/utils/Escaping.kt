package com.courier.android.utils

import java.net.URLEncoder

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
 * One URL path segment or query value, so a "/", "?" or "#" in an id can't change the request
 */
internal val String.urlEncoded: String
    get() = URLEncoder.encode(this, "UTF-8").replace("+", "%20")
