package com.example.contactapp.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.contactapp.model.Contact

@Dao
interface ContactDao {
    @Insert
    suspend fun addContact(contact: Contact) : Long
    @Update
    suspend fun updateContact(contact: Contact)
    @Delete
    suspend fun deleteContact(contact: Contact)
    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts() : List<Contact>
    @Query("SELECT * FROM contacts where contact_id ==:contactId")
    suspend fun getContactById(contactId: Long) : Contact
}