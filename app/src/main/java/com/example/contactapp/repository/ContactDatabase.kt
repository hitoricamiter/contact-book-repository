package com.example.contactapp.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.contactapp.model.Contact

@Database(entities = [Contact::class], version = 2)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun getContactDao() : ContactDao
}