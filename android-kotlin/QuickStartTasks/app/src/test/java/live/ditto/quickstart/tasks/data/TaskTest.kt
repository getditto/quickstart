package live.ditto.quickstart.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTest {

    @Test
    fun fromJson_roundTrips_throughToMap() {
        val original = Task(
            _id = "abc-123",
            title = "Buy milk",
            done = true,
            deleted = false
        )

        val json = mapToJsonString(original.toMap())
        val parsed = Task.fromJson(json)

        assertEquals(original, parsed)
    }

    @Test
    fun fromJson_handlesMalformedJson_withFallback() {
        val parsed = Task.fromJson("not json at all")

        // Falls back to an empty-titled task with a generated id rather than throwing.
        assertEquals("", parsed.title)
        assertFalse(parsed.done)
        assertFalse(parsed.deleted)
        assertTrue(parsed._id.isNotEmpty())
    }

    @Test
    fun fromJson_missingFields_useDefaults() {
        // Previously, missing "done"/"deleted" raised ClassCastException via `as Boolean`,
        // bypassing the JSONException catch. Now optBoolean returns the default.
        val parsed = Task.fromJson("""{"_id":"x","title":"only-title"}""")

        assertEquals("x", parsed._id)
        assertEquals("only-title", parsed.title)
        assertFalse(parsed.done)
        assertFalse(parsed.deleted)
    }

    @Test
    fun fromJson_typeMismatch_doesNotCrash() {
        // "done" delivered as the string "true" — optBoolean coerces, so we don't crash.
        val parsed = Task.fromJson(
            """{"_id":"x","title":"t","done":"true","deleted":"false"}"""
        )

        assertEquals("x", parsed._id)
        assertEquals("t", parsed.title)
        assertTrue(parsed.done)
        assertFalse(parsed.deleted)
    }

    @Test
    fun defaultId_isUnique() {
        val a = Task(title = "first")
        val b = Task(title = "second")

        assertNotEquals(a._id, b._id)
    }

    private fun mapToJsonString(map: Map<String, Any?>): String =
        org.json.JSONObject(map).toString()
}
