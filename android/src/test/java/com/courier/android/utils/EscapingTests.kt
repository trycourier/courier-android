package com.courier.android.utils

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

// Ids go into GraphQL text and URL paths, so a quote or slash in one must not change the request
class EscapingTests {

    // The query as the server reads it, after the JSON wrapper from toGraphQuery() is parsed
    private fun sentQuery(id: String): String {
        val body = "mutation { read(messageId: ${id.graphQLArgument}) }".toGraphQuery()
        return JsonParser.parseString(body).asJsonObject.get("query").asString
    }

    @Test
    fun plainIdMatchesTheExistingQueryFormat() {
        assertEquals("\\\"1-abc\\\"", "1-abc".graphQLArgument)
        assertEquals("mutation { read(messageId: \"1-abc\") }", sentQuery("1-abc"))
    }

    @Test
    fun quoteInAnIdStaysInsideTheString() {
        assertEquals("mutation { read(messageId: \"a\\\"b\") }", sentQuery("a\"b"))
    }

    @Test
    fun backslashInAnIdStaysInsideTheString() {
        assertEquals("mutation { read(messageId: \"a\\\\\") }", sentQuery("a\\"))
    }

    @Test
    fun controlCharactersAreEscaped() {
        assertEquals("\"a\\nb\\u0001\"", "a\nb\u0001".graphQLString)
    }

    @Test
    fun urlEncodingKeepsAnIdInOnePathSegment() {
        assertEquals("..%2Fadmin", "../admin".urlEncoded)
        assertEquals("a%20b%3Fc%23d", "a b?c#d".urlEncoded)
        assertEquals("plain-id_1", "plain-id_1".urlEncoded)
    }

}
