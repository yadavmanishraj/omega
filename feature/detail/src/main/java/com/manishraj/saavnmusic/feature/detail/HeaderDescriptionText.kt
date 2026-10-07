package com.manishraj.saavnmusic.feature.detail

/**
 * Cleans a Detail header description for display (impeccable
 * critique fix, P2). Upstream hands the header ONE baked string —
 * `RawMappers` copies `headerDesc` verbatim into
 * `Album.description` / `Playlist.description` — and that string
 * carries the data layer's mess with it: album headers repeat the
 * subtitle's artist enumeration as a trailing segment
 * ("2013 · Hindi Album · Jeet Gannguli, Mithoon, and Ankit Tiwari"
 * under the subtitle "Jeet Gannguli, Mithoon, and Ankit Tiwari"),
 * and playlist "descriptions" can be pure credit metadata
 * ("Artists On Cover: Sidharth Malhotra & Kiara Advani"). There is
 * no structured form below this layer to fix instead, so the
 * cleanup lives where the header is assembled for display.
 *
 * Rules, applied per "·"-separated segment:
 * - Credit segments are dropped: a segment that starts a credit
 *   label ("Artists On Cover:", "Cover:", "Photo:", "Label:", …)
 *   or a ℗ / © / copyright line is non-editorial metadata. A credit
 *   label appearing mid-segment cuts everything from the label on.
 * - A segment that is ONLY an enumeration of the subtitle's artists
 *   (same names, any conjunction style) is dropped as a duplicate;
 *   the remaining facts (year · type · language) survive.
 * - Nothing editorial is rewritten: if no segment is dropped or
 *   cut, the input returns byte-identical. When the string IS
 *   rebuilt, segments are trimmed and rejoined with " · ", so the
 *   result never has dangling separators or doubled spaces.
 *
 * Returns null when nothing editorial remains — [SongListHeader]
 * already hides a null/blank description.
 */
internal fun cleanHeaderDescription(
    description: String?,
    subtitle: String?,
): String? {
    if (description.isNullOrBlank()) return null
    val subtitleArtists = parseArtistNames(subtitle)
    var changed = false
    val kept = mutableListOf<String>()
    for (rawSegment in description.split('·')) {
        val segment = rawSegment.trim()
        if (segment.isEmpty()) {
            // Separator debris (leading/trailing/doubled "·") —
            // dropping it is a change, so hygiene applies on rebuild.
            changed = true
            continue
        }
        val editorial = stripCredit(segment)
        if (editorial == null) {
            changed = true
            continue
        }
        if (editorial != segment) changed = true
        if (isDuplicateArtistEnumeration(editorial, subtitleArtists)) {
            changed = true
            continue
        }
        kept += editorial
    }
    if (!changed) return description
    return kept.joinToString(" · ").ifBlank { null }
}

/** Credit labels whose content is metadata, never editorial copy. */
private val CreditLabelPattern =
    Regex(
        "\\b(artists on cover|cover|photo|photography|photographer|image|label)\\s*:",
        RegexOption.IGNORE_CASE,
    )

/**
 * Returns the editorial part of [segment], or null when the whole
 * segment is a credit line. A ℗ / © / copyright line is credit in
 * full; a credit LABEL mid-segment ("… Label: Sony Music") cuts
 * the segment at the label, keeping any editorial prefix.
 */
private fun stripCredit(segment: String): String? {
    if (segment.startsWith('℗') || segment.startsWith('©')) return null
    if (segment.startsWith("copyright", ignoreCase = true)) return null
    val match = CreditLabelPattern.find(segment) ?: return segment
    if (match.range.first == 0) return null
    return segment
        .substring(0, match.range.first)
        .trim()
        .trimEnd(',', ';', '·', ' ')
        .ifBlank { null }
}

/** Splits an artist enumeration on ",", "&", and the word "and". */
private val ArtistNameSplitter = Regex("[,&]|\\band\\b", RegexOption.IGNORE_CASE)

/**
 * Normalizes an enumeration into comparable artist names:
 * lowercased, whitespace-collapsed, punctuation-trimmed. Both the
 * subtitle and a description segment go through the SAME parse, so
 * conjunction style (", and" vs "&") never affects the comparison.
 */
private fun parseArtistNames(text: String?): Set<String> {
    if (text.isNullOrBlank()) return emptySet()
    return text
        .split(ArtistNameSplitter)
        .map {
            it
                .trim()
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim('.', ' ')
        }.filter { it.isNotEmpty() }
        .toSet()
}

/**
 * True when [segment] consists of nothing but artist names that
 * all appear in the subtitle's enumeration — a restatement of the
 * subtitle, not a fact about the release. Prose can never qualify:
 * every comma/"and"-separated fragment would have to exactly equal
 * a subtitle artist name.
 */
private fun isDuplicateArtistEnumeration(
    segment: String,
    subtitleArtists: Set<String>,
): Boolean {
    if (subtitleArtists.isEmpty()) return false
    val names = parseArtistNames(segment)
    return names.isNotEmpty() && names.all { it in subtitleArtists }
}
