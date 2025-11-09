package com.example.contactapp.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.databinding.ContactItemBinding
import com.example.contactapp.model.Contact

class ContactAdapter(
    private val contacts: MutableList<Contact>,
    private val onItemClick: (Contact, Int) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(val binding: ContactItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactViewHolder {

        val binding = ContactItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)

    }

    override fun onBindViewHolder(
        holder: ContactViewHolder,
        position: Int
    ) {
        val contact = contacts[position]

        holder.binding.firstName.text = contact.firstName
        holder.binding.lastName.text = contact.lastName
        holder.binding.email.text = contact.email
        holder.binding.phone.text = contact.phone

        holder.itemView.setOnClickListener {
            onItemClick(contact, position)
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

}