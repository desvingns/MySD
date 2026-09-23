package dev.mysd.android.product

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ProductBackupPolicyTest {
    @Test
    fun localSaveDomainsAreExcludedFromCloudAndDeviceTransfer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        val expected = setOf(
            "root", "file", "database", "sharedpref", "external",
            "device_root", "device_file", "device_database", "device_sharedpref",
        )
        val excluded = mutableMapOf<String, MutableSet<String>>()
        var section: String? = null
        context.resources.getXml(R.xml.product_data_extraction_rules).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer" -> section = parser.name
                        "exclude" -> {
                            assertEquals(".", parser.getAttributeValue(null, "path"))
                            excluded.getOrPut(requireNotNull(section)) { mutableSetOf() }
                                .add(parser.getAttributeValue(null, "domain"))
                        }
                    }
                }
                parser.next()
            }
        }
        assertEquals(mapOf("cloud-backup" to expected, "device-transfer" to expected), excluded)
    }
}
