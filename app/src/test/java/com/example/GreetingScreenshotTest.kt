package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.reelscraper.ui.screens.SampleSiteCard
import com.example.reelscraper.ui.viewmodel.SampleSite
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sample = SampleSite(
        title = "Peach Blender Project",
        description = "Open-source Big Buck Bunny video source page",
        url = "https://peach.blender.org/",
        tag = "Videos"
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        SampleSiteCard(sample = sample, onSelect = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
