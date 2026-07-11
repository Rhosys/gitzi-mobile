package ch.rhosys.gitzi.domain.model

/**
 * Where and how the app reaches a Gitzi backend. There is no local daemon on
 * mobile — every interaction goes through a deployed Gitzi server, which in
 * turn talks to whatever model provider the operator configured (their own
 * key, an on-prem gitzi install, or a self-hosted Ollama/vLLM box).
 */
data class ConnectionSettings(
    val serverUrl: String = "",
    val apiToken: String = "",
    /** Debug builds only — see SwitchableGitziRepository. Ignored in release. */
    val useMockData: Boolean = true,
    val isPaired: Boolean = false,
)
