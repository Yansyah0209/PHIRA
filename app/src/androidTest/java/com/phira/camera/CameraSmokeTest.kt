package com.phira.camera

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraSmokeTest {
    @get:Rule val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    @Test fun cameraAndSettingsAreReachable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        ActivityScenario.launch(MainActivity::class.java).use {
            assertTrue("Camera must initialize", device.wait(Until.hasObject(By.text("ON DEVICE")), 30000))
            device.findObject(By.desc("Take photograph")).click()
            assertTrue("Photo must save", device.wait(Until.hasObject(By.text("Saved to Pictures / PHIRA")), 15000))
            // Gradle uninstalls the app after tests; shell-owned Downloads survive that cleanup.
            device.executeShellCommand("screencap -p /sdcard/Download/PHIRA-camera.png")
            device.findObject(By.desc("Camera settings")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Make it yours")), 5000))
            assertNotNull(device.findObject(By.text("Auto capture")))
            device.executeShellCommand("screencap -p /sdcard/Download/PHIRA-settings.png")
            device.pressBack()
            assertTrue(device.wait(Until.hasObject(By.desc("Take photograph")), 5000))
        }
    }
}
