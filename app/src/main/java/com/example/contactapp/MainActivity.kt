package com.example.contactapp

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.contactapp.databinding.ActivityMainBinding
import com.example.contactapp.databinding.ContactPopUpBinding
import com.example.contactapp.model.Contact
import com.example.contactapp.repository.ContactDatabase
import com.example.contactapp.util.ContactAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private val contacts = mutableListOf<Contact>()
    private lateinit var contactDatabase: ContactDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactDatabase =
            Room.databaseBuilder(applicationContext, ContactDatabase::class.java, "ContactsDB").fallbackToDestructiveMigration()
                .build()

        contactAdapter = ContactAdapter(contacts) { contact, position ->
            addAndEditContacts(isUpdate = true, contact = contact, position = position)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            itemAnimator = DefaultItemAnimator()
            adapter = contactAdapter
        }

        lifecycleScope.launch {
            val contactDB = withContext(Dispatchers.IO) {
                contactDatabase.getContactDao().getAllContacts()
            }
            contacts.clear()
            contacts.addAll(contactDB)
            contactAdapter.notifyDataSetChanged()
        }

        binding.floatingActionButton.setOnClickListener {
            addAndEditContacts(isUpdate = false)
        }

    }

    private fun addAndEditContacts(
        isUpdate: Boolean,
        contact: Contact? = null,
        position: Int = -1
    ) {
        val addContactBinding = ContactPopUpBinding.inflate(LayoutInflater.from(this))

        contact?.let {
            addContactBinding.firstNameEdit.setText(it.firstName)
            addContactBinding.lastNameEdit.setText(it.lastName)
            addContactBinding.emailEdit.setText(it.email)
            addContactBinding.phoneEdit.setText(it.phone)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(addContactBinding.root)
            .setCancelable(false)
            .setPositiveButton(if (isUpdate) "Update" else "Save", null)
            .setNegativeButton(if (isUpdate) "Delete" else "Cancel") { dialogInterface, _ ->
                if (isUpdate && contact != null) deleteContact(contact, position)
                else dialogInterface.cancel()
            }
            .create()

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = addContactBinding.firstNameEdit.text.toString().trim()
            val lastName = addContactBinding.lastNameEdit.text.toString().trim()
            val email = addContactBinding.emailEdit.text.toString().trim()
            val phone = addContactBinding.phoneEdit.text.toString().trim()

            when {
                name.isEmpty() -> Toast.makeText(this, "Enter contact name!", Toast.LENGTH_SHORT)
                    .show()

                lastName.isEmpty() -> Toast.makeText(
                    this,
                    "Enter contact last name!",
                    Toast.LENGTH_SHORT
                ).show()

                email.isEmpty() -> Toast.makeText(this, "Enter contact  email!", Toast.LENGTH_SHORT)
                    .show()

                phone.isEmpty() -> Toast.makeText(this, "Enter phone number!", Toast.LENGTH_SHORT)
                    .show()

                else -> {
                    dialog.dismiss()
                    if (isUpdate && contact != null) {
                        updateContact(name, lastName, email, phone, position)
                    } else {
                        createContact(name, lastName, email, phone)
                    }
                }
            }
        }
    }

    private fun deleteContact(contact: Contact, position: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                contactDatabase.getContactDao().deleteContact(contact)
            }
            contacts.removeAt(position)
            contactAdapter.notifyItemRemoved(position)
        }
    }

    private fun updateContact(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        position: Int
    ) {
        val contact = contacts[position].apply {
            this.firstName = firstName
            this.lastName = lastName
            this.email = email
            this.phone = phone
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                contactDatabase.getContactDao().updateContact(contact)
            }
            contacts[position] = contact
            contactAdapter.notifyItemChanged(position)
        }

    }

    private fun createContact(name: String, lastName: String, email: String, phone: String) {
        lifecycleScope.launch {
            val newContact = withContext(Dispatchers.IO) {
                val id = contactDatabase.getContactDao()
                    .addContact(Contact(0, name, lastName, email, phone))
                contactDatabase.getContactDao().getContactById(id)
            }

            newContact?.let {
                contacts.add(0, it)
                contactAdapter.notifyItemInserted(0)
                binding.recyclerView.scrollToPosition(0)
            }
        }
    }


}