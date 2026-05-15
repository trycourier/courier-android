package PACKAGE_NAME

/**
 * Local credentials for the example app and instrumented tests.
 * Copy to [env_setup.sh] destinations or run `./env_setup.sh`.
 * Mirrors courier-ios `EnvSample.swift`.
 */
class Env {
    companion object {
        const val COURIER_USER_ID = "XXXXX"
        const val COURIER_AUTH_KEY = "XXXXX"
        const val COURIER_CLIENT_KEY = "XXXXX"
        const val COURIER_BRAND_ID = "XXXXX"
        const val COURIER_PREFERENCE_TOPIC_ID = "XXXXX"
        const val COURIER_MESSAGE_TEMPLATE_ID = "XXXXX"
        const val FIREBASE_API_KEY = "XXXXXX"
        const val FIREBASE_APP_ID = "XXXXXX"
        const val FIREBASE_PROJECT_ID = "XXXXXX"
        const val FIREBASE_GCM_SENDER_ID = "XXXXXX"
    }
}
