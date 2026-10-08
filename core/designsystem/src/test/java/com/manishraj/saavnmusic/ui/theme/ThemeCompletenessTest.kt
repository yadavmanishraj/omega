package com.manishraj.saavnmusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Completeness gate (visual-uplift spec §1, verification hook V1):
 * neither static scheme may resolve ANY color role to the baseline
 * Material value, because an unassigned role is exactly how the
 * pre-uplift theme leaked stock grays (#211F26 nav bar, #2B2930 slab)
 * into a brand scheme that never chose them.
 *
 * The one honest exception: five LIGHT roles whose spec value IS
 * white — identical to the baseline's white. For those, the gate
 * asserts the spec value itself, so the invariant enforced is the
 * real one: no role sits at baseline unless §1 assigns exactly that
 * value. Deleting any other assignment from Theme.kt fails here.
 */
class ThemeCompletenessTest {
    @Test
    fun darkSchemeAssignsEveryRole() {
        assertComplete(
            omega = DarkColors,
            baseline = darkColorScheme(),
            intentionalMatches = emptyMap(),
        )
    }

    @Test
    fun lightSchemeAssignsEveryRole() {
        assertComplete(
            omega = LightColors,
            baseline = lightColorScheme(),
            intentionalMatches = INTENTIONAL_LIGHT_MATCHES,
        )
    }

    /** The values the whole wave hangs on, pinned literally (spec §1.2/§1.3). */
    @Test
    fun chromeAndGroupedRolesCarrySpecValues() {
        assertEquals(Color(0xFF1D1D37), DarkColors.surfaceContainer)
        assertEquals(Color(0xFF282742), DarkColors.surfaceContainerHigh)
        assertEquals(Color(0xFF33324E), DarkColors.surfaceContainerHighest)
        assertEquals(Color(0xFF0B0B1F), DarkColors.surfaceContainerLowest)
        assertEquals(Color(0xFFEBEDF7), LightColors.surfaceContainer)
        assertEquals(Color(0xFFE6E8F1), LightColors.surfaceContainerHigh)
        assertEquals(Color(0xFFE0E2EB), LightColors.surfaceContainerHighest)
        assertEquals(Color(0xFFF7F9FF), LightColors.surface)
        assertEquals(Color(0xFF72000E), DarkColors.errorContainer)
        assertEquals(Color(0xFFFFC4B6), LightColors.errorContainer)
    }

    private fun assertComplete(
        omega: ColorScheme,
        baseline: ColorScheme,
        intentionalMatches: Map<String, Color>,
    ) {
        val omegaRoles = rolesOf(omega)
        val baselineRoles = rolesOf(baseline)
        assertEquals("role inventory drifted", baselineRoles.keys, omegaRoles.keys)
        for ((role, value) in omegaRoles) {
            val specValue = intentionalMatches[role]
            if (specValue != null) {
                assertEquals("$role must carry its spec value", specValue, value)
            } else {
                assertNotEquals(
                    "$role still resolves to the baseline Material value",
                    baselineRoles.getValue(role),
                    value,
                )
            }
        }
    }

    private fun rolesOf(scheme: ColorScheme): Map<String, Color> =
        linkedMapOf(
            "primary" to scheme.primary,
            "onPrimary" to scheme.onPrimary,
            "primaryContainer" to scheme.primaryContainer,
            "onPrimaryContainer" to scheme.onPrimaryContainer,
            "inversePrimary" to scheme.inversePrimary,
            "secondary" to scheme.secondary,
            "onSecondary" to scheme.onSecondary,
            "secondaryContainer" to scheme.secondaryContainer,
            "onSecondaryContainer" to scheme.onSecondaryContainer,
            "tertiary" to scheme.tertiary,
            "onTertiary" to scheme.onTertiary,
            "tertiaryContainer" to scheme.tertiaryContainer,
            "onTertiaryContainer" to scheme.onTertiaryContainer,
            "background" to scheme.background,
            "onBackground" to scheme.onBackground,
            "surface" to scheme.surface,
            "onSurface" to scheme.onSurface,
            "surfaceVariant" to scheme.surfaceVariant,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "surfaceTint" to scheme.surfaceTint,
            "inverseSurface" to scheme.inverseSurface,
            "inverseOnSurface" to scheme.inverseOnSurface,
            "error" to scheme.error,
            "onError" to scheme.onError,
            "errorContainer" to scheme.errorContainer,
            "onErrorContainer" to scheme.onErrorContainer,
            "outline" to scheme.outline,
            "outlineVariant" to scheme.outlineVariant,
            "scrim" to scheme.scrim,
            "surfaceBright" to scheme.surfaceBright,
            "surfaceDim" to scheme.surfaceDim,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest,
            "surfaceContainerLow" to scheme.surfaceContainerLow,
            "surfaceContainerLowest" to scheme.surfaceContainerLowest,
            "primaryFixed" to scheme.primaryFixed,
            "primaryFixedDim" to scheme.primaryFixedDim,
            "onPrimaryFixed" to scheme.onPrimaryFixed,
            "onPrimaryFixedVariant" to scheme.onPrimaryFixedVariant,
            "secondaryFixed" to scheme.secondaryFixed,
            "secondaryFixedDim" to scheme.secondaryFixedDim,
            "onSecondaryFixed" to scheme.onSecondaryFixed,
            "onSecondaryFixedVariant" to scheme.onSecondaryFixedVariant,
            "tertiaryFixed" to scheme.tertiaryFixed,
            "tertiaryFixedDim" to scheme.tertiaryFixedDim,
            "onTertiaryFixed" to scheme.onTertiaryFixed,
            "onTertiaryFixedVariant" to scheme.onTertiaryFixedVariant,
        )

    private companion object {
        /**
         * Light roles whose §1.3 spec value is white, which coincides
         * with the baseline's white. Every other light role — and every
         * dark role — must differ from baseline.
         */
        val INTENTIONAL_LIGHT_MATCHES: Map<String, Color> =
            mapOf(
                "onPrimary" to Color.White,
                "onSecondary" to Color.White,
                "onTertiary" to Color.White,
                "onError" to Color.White,
                "surfaceContainerLowest" to Color.White,
            )
    }
}
