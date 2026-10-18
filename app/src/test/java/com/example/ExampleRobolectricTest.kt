package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiApiClient
import com.example.data.ai.OpenAICompatibleClient
import com.example.data.storage.PreferencesManager
import com.example.domain.model.ActionType
import com.example.domain.model.MascotState
import com.example.domain.planner.TaskPlanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nafsi AI", appName)
    }

    @Test
    fun `test bengali calculator multi-step planning`() = runBlocking {
        val planner = TaskPlanner(GeminiApiClient(), OpenAICompatibleClient())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        val task = planner.planTask("ক্যালকুলেটরে দুই যোগ দুই করো", prefs.providerConfig.value)

        assertNotNull(task)
        assertEquals(6, task.steps.size)
        assertEquals(ActionType.LAUNCH_APP, task.steps[0].action)
        assertEquals("Calculator", task.steps[0].target)
        assertEquals(ActionType.TAP_ELEMENT, task.steps[1].action)
        assertEquals("2", task.steps[1].target)
        assertEquals(ActionType.VERIFY_RESULT, task.steps[5].action)
        assertEquals("4", task.steps[5].target)
    }

    @Test
    fun `test natural intent app launching without keywords`() = runBlocking {
        val planner = TaskPlanner(GeminiApiClient(), OpenAICompatibleClient())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        val task = planner.planTask("দয়া করে ফেসবুকটা একটু খোলো তো", prefs.providerConfig.value)

        assertNotNull(task)
        assertTrue(task.steps.isNotEmpty())
        assertEquals(ActionType.LAUNCH_APP, task.steps[0].action)
        assertEquals("Facebook", task.steps[0].target)
    }

    @Test
    fun `test natural conversation without rigid keywords`() = runBlocking {
        val planner = TaskPlanner(GeminiApiClient(), OpenAICompatibleClient())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        val task = planner.planTask("তুমি কে এবং তোমার নাম কী?", prefs.providerConfig.value)

        assertNotNull(task)
        assertTrue(task.resultMessage?.contains("নাফসি") == true)
    }

    @Test
    fun `test mascot state labels exist`() {
        val states = MascotState.values()
        assertTrue(states.isNotEmpty())
        assertEquals("স্বাভাবিক অবস্থা", MascotState.IDLE.bengaliLabel)
        assertEquals("শুনছে...", MascotState.LISTENING.bengaliLabel)
        assertEquals("কাজ সম্পন্ন!", MascotState.SUCCESS.bengaliLabel)
    }
}
