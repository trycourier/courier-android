package com.courier.example

import com.courier.android.client.CourierClient

data class CourierEnvironment(val label: String, val urls: CourierClient.ApiUrls?) {

    companion object {
        val PRODUCTION = CourierEnvironment("Production", CourierClient.ApiUrls.us())
        val PRODUCTION_EU = CourierEnvironment("Production EU", CourierClient.ApiUrls.eu())
        val CUSTOM = CourierEnvironment("Custom", null)

        // Extra environments come from the gitignored Env.kt (see localEnvironments below)
        fun all(): List<CourierEnvironment> {
            return listOf(PRODUCTION, PRODUCTION_EU) + Env.localEnvironments + listOf(CUSTOM)
        }

        // A saved environment this build doesn't define keeps its saved URLs, so it shows as Custom
        fun fromLabel(label: String): CourierEnvironment {
            return all().firstOrNull { it.label == label } ?: CUSTOM
        }
    }

}

/**
 * Extra environments for local builds. Declare them in the companion object of Env.kt and the
 * environment picker lists them between Production EU and Custom:
 *
 * ```
 * val localEnvironments = listOf(
 *     CourierEnvironment("My Environment", CourierClient.ApiUrls(
 *         rest = "https://…", graphql = "https://…/client/q",
 *         inboxGraphql = "https://…/q", inboxWebSocket = "wss://…"
 *     ))
 * )
 * ```
 *
 * A member always wins over an extension, so an Env.kt that declares it overrides this empty
 * default, and one that doesn't (like EnvSample.kt) keeps compiling.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
val Env.Companion.localEnvironments: List<CourierEnvironment>
    get() = emptyList()
