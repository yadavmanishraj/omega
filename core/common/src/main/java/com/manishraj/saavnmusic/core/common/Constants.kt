package com.manishraj.saavnmusic.core.common

/**
 * The one upstream endpoint the app talks to: JioSaavn's own `api.php`,
 * exactly as the jiosaavn-api repository builds its calls (see
 * docs/sdlc/UPSTREAM_SPEC.md). There is no hosted wrapper instance.
 */
const val JIOSAAVN_API_ENDPOINT = "https://www.jiosaavn.com/api.php"
