package cat.vira.android.core.backend

/// Adreça del backend i clau pública (`Backend.plist` de l'iPhone).
///
/// Són **les mateixes** que l'iPhone: un compte, un servidor, i les mateixes taules als dos mòbils.
/// La clau `anon` és pública per disseny —va dins de l'app de tothom i tota la protecció real són
/// les polítiques RLS—, així que pot viure al codi, com al plist de l'iPhone. La clau
/// `service_role` **no entra mai** aquí ni enlloc del client.
object ViraBackendConfig {

    data class Values(val baseUrl: String, val anonKey: String) {
        val authUrl: String get() = "$baseUrl/auth/v1"
        fun rest(table: String) = "$baseUrl/rest/v1/$table"
        fun rpc(function: String) = "$baseUrl/rest/v1/rpc/$function"
    }

    /// `null` deixaria l'app en mode local, com l'iPhone amb el plist de plantilla.
    val current: Values? = Values(
        baseUrl = "https://yhqhtgnkuvsxemyqjpiy.supabase.co",
        anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlocWh0Z25rdXZzeGVteXFqcGl5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU5OTI4MzUsImV4cCI6MjEwMTU2ODgzNX0.oDI55OH-ZmaXZ8tY1u3Mt2CvmZsecuY18bR1zp6wWHk",
    )

    val isConfigured: Boolean get() = current != null

    /// Tornada dels enllaços de correu i d'OAuth: `vira://auth-callback`, la mateixa de l'iPhone, que
    /// ja és a la llista de redireccions permeses de Supabase.
    const val CALLBACK_SCHEME = "vira"
    const val CALLBACK_HOST = "auth-callback"
    const val CALLBACK_URL = "$CALLBACK_SCHEME://$CALLBACK_HOST"
}
