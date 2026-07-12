package ch.rhosys.gitzi

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test — verifies the app launches without crashing on a minified release build.
 *
 * Run against the release variant to validate ProGuard/R8 rules:
 *   ./gradlew connectedReleaseAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesWithoutCrashing() {
        composeRule.waitForIdle()
    }
}
