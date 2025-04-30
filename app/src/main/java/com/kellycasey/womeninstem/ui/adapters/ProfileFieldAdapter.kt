package com.kellycasey.womeninstem.ui.profile

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kellycasey.womeninstem.databinding.ItemProfileFieldBinding

/**
 * Represents one editable field on the profile screen.
 */
data class ProfileField(
    val key: String,      // e.g. "name", "subject", "age", etc.
    var value: String,    // current value (always kept as a String here)
    val label: String     // human-readable ("Name", "Subject", "Age", etc.)
)

class ProfileFieldAdapter(
    private val fields: List<ProfileField>
) : RecyclerView.Adapter<ProfileFieldAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemProfileFieldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(field: ProfileField) {
            binding.textLabel.text = field.label
            binding.editValue.setText(field.value)

            // If this is the "age" field, switch to numeric input
            if (field.key == "age") {
                binding.editValue.inputType = InputType.TYPE_CLASS_NUMBER
            } else {
                binding.editValue.inputType = InputType.TYPE_CLASS_TEXT
            }

            // Avoid accumulating watchers
            binding.editValue.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    field.value = s?.toString() ?: ""
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProfileFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(fields[position])
    }

    override fun getItemCount(): Int = fields.size

    /** Expose updated values back to the fragment */
    fun getUpdatedFields(): List<ProfileField> = fields
}
