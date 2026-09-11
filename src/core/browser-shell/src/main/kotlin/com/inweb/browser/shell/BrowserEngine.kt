package com.inweb.browser.shell

/**
 * Port to the Chromium content layer.
 *
 * The UI and core logic program against this interface. The Chromium-backed
 * implementation is delivered by the engine adapter in the ui/ patch area on
 * build infrastructure (docs/PHASE2-INTEGRATION-PLAN.md). Until that adapter
 * ships, this port is architecture — not a claimed browsing capability
 * (MASTER-SPEC §57).
 */
interface BrowserEngine {
    fun loadUrl(tabId: String, url: String)
    fun goBack(tabId: String)
    fun goForward(tabId: String)
    fun stop(tabId: String)
    fun reload(tabId: String)
}

/** Engine events the shell observes (delivered by the engine adapter). */
interface EngineEvents {
    fun onPageStarted(tabId: String, url: String)
    fun onPageFinished(tabId: String, url: String, security: PageSecurityState)
    fun onProgress(tabId: String, percent: Int)
}
