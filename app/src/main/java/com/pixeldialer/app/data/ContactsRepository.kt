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

    suspend fun loadAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
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
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val starredIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)

            val seen = HashSet<String>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                if (!seen.add(id)) continue // de-dupe multiple numbers per contact for list view
                results.add(
                    Contact(
                        id = id,
                        displayName = cursor.getString(nameIdx) ?: "Unknown",
                        phoneNumber = cursor.getString(numberIdx) ?: "",
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
                return@withContext Contact(
                    id = cursor.getString(0) ?: "",
                    displayName = cursor.getString(1) ?: number,
                    phoneNumber = number,
                    photoUri = cursor.getString(2)
                )
            }
        }
        null
    }
}
