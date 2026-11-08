package com.lectures

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.util.NotificationPermissionHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationPermissionPersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        // Clear preferences for isolation
        context.getSharedPreferences("mustime_notification_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testNotificationPermissionPromptDefaultsToFalseAndPersistsTrue() {
        assertFalse(
            "Default prompted state must be false",
            NotificationPermissionHelper.hasPromptedNotificationPermission(context)
        )

        NotificationPermissionHelper.setPromptedNotificationPermission(context, true)

        assertTrue(
            "Prompted state must persist as true so app does not repeatedly prompt user",
            NotificationPermissionHelper.hasPromptedNotificationPermission(context)
        )
    }

    @Test
    fun testTaskBannerDismissalDefaultsToFalseAndPersistsTrue() {
        assertFalse(
            "Default task banner dismissal must be false",
            NotificationPermissionHelper.isTaskBannerDismissed(context)
        )

        NotificationPermissionHelper.setTaskBannerDismissed(context, true)

        assertTrue(
            "Task banner dismissal must persist so user is not repeatedly nagged",
            NotificationPermissionHelper.isTaskBannerDismissed(context)
        )
    }
}
