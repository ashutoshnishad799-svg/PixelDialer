package com.pixeldialer.app.data

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads real device contacts via ContactsContract.
 * Requires READ_CONTACTS permission — callers must check before invoking.
 */
class ContactsRepository(private val context: Context) {

    /**
     * Loads every phone-number row for every contact — NOT one row per
     * contact. A single person can have multiple numbers (Mobile + Home,
     * dual-SIM, work line, etc), and the previous version of this function
     * de-duplicated by CONTACT_ID, silently dropping every number after
     * the first one it saw for a given person. That's why search couldn't
     * find some people by number: their second/third number was never
     * loaded into the app's contact list at all, so there was nothing to
     * match against — the bug was in what got loaded, not in the filter.
     */
    suspend fun loadAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val starredIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)

            // Dedupe only exact (contactId, number) pairs — some devices/
            // sync sources emit the literal same number twice for one
            // contact. This does NOT collapse a contact's distinct numbers
            // into one, which is the behavior that was broken before.
            val seenPairs = HashSet<String>()
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIdx) ?: continue
                val number = cursor.getString(numberIdx)?.trim() ?: continue
                if (number.isEmpty()) continue

                val pairKey = "$contactId|${number.filter { it.isDigit() || it == '+' }}"
                if (!seenPairs.add(pairKey)) continue

                val typeLabel = phoneTypeLabel(cursor.getInt(typeIdx), cursor.getString(labelIdx))

                results.add(
                    Contact(
                        id = "$contactId:$number",
                        contactId = contactId,
                        displayName = cursor.getString(nameIdx) ?: "Unknown",
                        phoneNumber = number,
                        numberLabel = typeLabel,
                        photoUri = cursor.getString(photoIdx),
                        isFavorite = cursor.getInt(starredIdx) == 1
                    )
                )
            }
        }
        results
    }

    suspend fun lookupNameForNumber(number: String): Contact? = withContext(Dispatchers.IO) {
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getString(0) ?: ""
                return@withContext Contact(
                    id = "$contactId:$number",
                    contactId = contactId,
                    displayName = cursor.getString(1) ?: number,
                    phoneNumber = number,
                    photoUri = cursor.getString(2)
                )
            }
        }
        null
    }

    private fun phoneTypeLabel(type: Int, customLabel: String?): String = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work Fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home Fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "Pager"
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel?.takeIf { it.isNotBlank() } ?: "Other"
        else -> "Mobile"
    }

    private fun labelToPhoneType(label: String): Int = when (label) {
        "Mobile" -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        "Home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        "Work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
    }

    /**
     * Inserts a new contact via the standard three-row RawContacts/Data
     * batch-operation pattern (this is how every Android contacts-writing
     * app does it — there's no simpler single-call API). Requires
     * WRITE_CONTACTS permission — callers must check before invoking.
     */
    suspend fun insertContact(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        phoneLabel: String,
        email: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<android.content.ContentProviderOperation>()

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName.ifBlank { null })
                    .build()
            )

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, labelToPhoneType(phoneLabel))
                    .build()
            )

            if (email.isNotBlank()) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build()
                )
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }
}
