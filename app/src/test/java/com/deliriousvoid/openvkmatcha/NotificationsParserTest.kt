package com.deliriousvoid.openvkmatcha

import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationsParserTest {

    @Test
    fun testParseNotifications_copyPost_extractsAuthorFromProfiles() {
        val jsonString = """
        {
            "response": {
                "items": [
                    {
                        "type": "copy_post",
                        "date": 1700000000,
                        "parent": {
                            "id": 100,
                            "from_id": 12345,
                            "owner_id": 12345,
                            "text": "My original post"
                        },
                        "feedback": {
                            "count": 1,
                            "items": [
                                {
                                    "id": 500,
                                    "from_id": 98765,
                                    "owner_id": 98765,
                                    "text": "Cool post!"
                                }
                            ]
                        }
                    }
                ],
                "profiles": [
                    {
                        "id": 98765,
                        "first_name": "Ivan",
                        "last_name": "Petrov",
                        "photo_50": "https://example.com/photo.jpg"
                    }
                ],
                "groups": []
            }
        }
        """.trimIndent()

        val jsonObject = JSONObject(jsonString)
        val response = JsonParsers.parseNotifications(jsonObject)

        assertEquals(1, response.items.size)
        val notif = response.items[0]

        assertEquals("copy_post", notif.type)
        assertEquals(98765, notif.authorId)
        assertEquals("Ivan Petrov", notif.authorName)
        assertEquals("https://example.com/photo.jpg", notif.authorAvatar)
    }

    @Test
    fun testParseNotifications_copyPost_jsonArrayFeedback() {
        val jsonString = """
        {
            "response": {
                "items": [
                    {
                        "type": "copy_post",
                        "date": 1700000000,
                        "parent": {
                            "id": 100,
                            "from_id": 12345,
                            "owner_id": 12345,
                            "text": "My original post"
                        },
                        "feedback": [
                            {
                                "id": 500,
                                "from_id": 77777,
                                "owner_id": 77777
                            }
                        ]
                    }
                ],
                "profiles": [
                    {
                        "id": 77777,
                        "first_name": "Pavel",
                        "last_name": "Durov",
                        "photo_50": "https://example.com/pavel.jpg"
                    }
                ],
                "groups": []
            }
        }
        """.trimIndent()

        val jsonObject = JSONObject(jsonString)
        val response = JsonParsers.parseNotifications(jsonObject)

        assertEquals(1, response.items.size)
        val notif = response.items[0]

        assertEquals(77777, notif.authorId)
        assertEquals("Pavel Durov", notif.authorName)
    }

    @Test
    fun testParseNotifications_copyPost_singleProfileFallback() {
        val jsonString = """
        {
            "response": {
                "items": [
                    {
                        "type": "copy_post",
                        "date": 1700000000,
                        "parent": {
                            "id": 100,
                            "from_id": 12345,
                            "owner_id": 12345,
                            "text": "My original post"
                        },
                        "feedback": {
                            "to_id": 12345
                        }
                    }
                ],
                "profiles": [
                    {
                        "id": 88888,
                        "first_name": "Anna",
                        "last_name": "Sidorova",
                        "photo_50": "https://example.com/anna.jpg"
                    }
                ],
                "groups": []
            }
        }
        """.trimIndent()

        val jsonObject = JSONObject(jsonString)
        val response = JsonParsers.parseNotifications(jsonObject)

        assertEquals(1, response.items.size)
        val notif = response.items[0]

        assertEquals(88888, notif.authorId)
        assertEquals("Anna Sidorova", notif.authorName)
    }
}
