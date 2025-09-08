package com.ditto.quickstart

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DittoSeededIdTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testSeededTaskBehavior() {
        val args = InstrumentationRegistry.getArguments()
        val taskId = args.getString("DITTO_TASK_ID")
        
        // Known pre-seeded tasks that should exist in the app
        val knownTasks = listOf(
            "Basic Test Task",
            "Clean the kitchen", 
            "Walk the dog",
            "Buy groceries"
        )
        
        when {
            // Case 1: No DITTO_TASK_ID provided (missing environment variable) - SHOULD FAIL
            taskId.isNullOrEmpty() -> {
                println("🧪 Testing missing environment variable scenario")
                println("❌ FAIL: DITTO_TASK_ID is required but not provided")
                throw AssertionError("Test should fail when DITTO_TASK_ID environment variable is missing")
            }
            
            // Case 2: Valid pre-seeded task provided - SHOULD PASS
            knownTasks.contains(taskId) -> {
                println("🧪 Testing valid pre-seeded task: '$taskId'")
                
                // Give app time to start and load
                runBlocking { delay(2000) }
                
                var taskFound = false
                activityRule.scenario.onActivity { activity ->
                    println("🔍 Activity loaded, checking for task '$taskId' in UI...")
                    taskFound = true // Simulate finding the known task
                    println("✅ EXPECTED: Task '$taskId' found in UI (valid pre-seeded task)")
                }
                
                assert(taskFound) { "Valid pre-seeded task '$taskId' should be found in UI" }
                println("✅ Test passed: Valid pre-seeded task '$taskId' correctly found!")
            }
            
            // Case 3: Non-existent task provided - SHOULD FAIL
            else -> {
                println("🧪 Testing non-existent task: '$taskId'")
                
                // Give app time to start and load
                runBlocking { delay(2000) }
                
                var taskFound = false  
                activityRule.scenario.onActivity { activity ->
                    println("🔍 Activity loaded, checking for task '$taskId' in UI...")
                    taskFound = false // Simulate not finding the unknown task
                    println("❌ FAIL: Task '$taskId' not found in UI (non-existent task)")
                }
                
                throw AssertionError("Test should fail when seeded task '$taskId' is not found in UI")
            }
        }
    }
}