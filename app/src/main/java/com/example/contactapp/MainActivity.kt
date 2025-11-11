package com.example.contactapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.databinding.DataBindingUtil


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private val contacts = mutableListOf<Contact>()
    private lateinit var contactDatabase: ContactDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        contactDatabase =
            Room.databaseBuilder(applicationContext, ContactDatabase::class.java, "ContactsDB")
                .fallbackToDestructiveMigration()
                .build()

        contactAdapter = ContactAdapter(contacts) { contact, position ->
            addAndEditContacts(isUpdate = true, contact = contact, position = position)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            itemAnimator = DefaultItemAnimator()
            adapter = contactAdapter
        }

        val handler: MainActivityButtonHandler = MainActivityButtonHandler()
        binding.button = handler

        val swipeToDeleteCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val contact = contacts[position]
                    deleteContact(contact, position)
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val itemView = viewHolder.itemView
                    val paint = Paint().apply { color = Color.RED }

                    if (dX < 0) {
                        c.drawRect(
                            itemView.right.toFloat() + dX,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat(),
                            paint
                        )
                    }

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }
        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(binding.recyclerView)


        lifecycleScope.launch {
            val contactDB = withContext(Dispatchers.IO) {
                contactDatabase.getContactDao().getAllContacts()
            }
            contacts.clear()
            contacts.addAll(contactDB)
            contactAdapter.notifyDataSetChanged()
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

        val dialog = AlertDialog.Builder(this)
            .setView(addContactBinding.root)
            .setCancelable(false)
            .setPositiveButton(if (isUpdate) "Update" else "Save", null)
            .setNegativeButton(if (isUpdate) "Delete" else "Cancel") { dialogInterface, _ ->
                if (isUpdate && contact != null) deleteContact(contact, position)
                else dialogInterface.cancel()
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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

    inner class MainActivityButtonHandler() {

        fun onAddButtonClick(view: View) {
            addAndEditContacts(isUpdate = false)
        }

    }


}