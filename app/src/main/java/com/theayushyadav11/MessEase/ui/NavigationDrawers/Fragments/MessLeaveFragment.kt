package com.theayushyadav11.MessEase.ui.NavigationDrawers.Fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.theayushyadav11.MessEase.Models.MessLeave
import com.theayushyadav11.MessEase.R
import com.theayushyadav11.MessEase.databinding.FragmentMessLeaveBinding
import com.theayushyadav11.MessEase.utils.Constants.Companion.firestoreReference
import com.theayushyadav11.MessEase.utils.Mess
import java.util.*
import java.text.SimpleDateFormat

class MessLeaveFragment : Fragment() {

    private lateinit var binding: FragmentMessLeaveBinding
    private lateinit var mess: Mess
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessLeaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mess = Mess(requireContext())
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnFullDay.setOnClickListener {
            showDatePicker("FULL_DAY")
        }
        binding.btnMealSkip.setOnClickListener {
            showDatePicker("MEAL_SKIP")
        }
        binding.btnEmergency.setOnClickListener {
            submitEmergencyLeave()
        }
    }

    private fun showDatePicker(type: String) {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                if (type == "MEAL_SKIP") {
                    showMealSelection()
                } else {
                    performBufferCheck(type, "")
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showMealSelection() {
        val meals = arrayOf("Breakfast", "Lunch", "Dinner")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Meal")
            .setItems(meals) { _, which ->
                performBufferCheck("MEAL_SKIP", meals[which])
            }
            .show()
    }

    private fun performBufferCheck(type: String, meal: String) {
        val currentTime = System.currentTimeMillis()
        
        // Meal start times (Assume defaults if not set)
        val mealStartTime = when (meal) {
            "Breakfast" -> 8 // 8 AM
            "Lunch" -> 13   // 1 PM
            "Dinner" -> 20  // 8 PM
            else -> 0       // For Full Day, consider start of day (0 AM)
        }

        val targetCalendar = calendar.clone() as Calendar
        targetCalendar.set(Calendar.HOUR_OF_DAY, mealStartTime)
        targetCalendar.set(Calendar.MINUTE, 0)
        targetCalendar.set(Calendar.SECOND, 0)
        
        val targetTime = targetCalendar.timeInMillis
        val diffHours = (targetTime - currentTime) / (1000 * 60 * 60)

        when (type) {
            "FULL_DAY" -> {
                if (diffHours >= 48) {
                    saveLeave(type, "APPROVED", false)
                } else {
                    showDeadlinePassed(type)
                }
            }
            "MEAL_SKIP" -> {
                if (diffHours >= 24) {
                    saveLeave(type, "APPROVED", false, meal)
                } else {
                    showDeadlinePassed(type)
                }
            }
        }
    }

    private fun showDeadlinePassed(type: String) {
        binding.tvStatus.text = "Deadline Passed for this date!"
        binding.tvStatus.visibility = View.VISIBLE
        binding.btnEmergency.visibility = View.VISIBLE
    }

    private fun submitEmergencyLeave() {
        saveLeave("EMERGENCY", "PENDING_APPROVAL", true)
    }

    private fun saveLeave(type: String, status: String, isException: Boolean, meal: String = "") {
        mess.addPb("Saving Leave...")
        val user = mess.getUser()
        val leaveId = firestoreReference.collection("MessLeaves").document().id
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = sdf.format(calendar.time)

        val leave = MessLeave(
            id = leaveId,
            uid = user.uid,
            userName = user.name,
            type = type,
            date = dateString,
            meal = meal,
            status = status,
            exceptionCase = isException,
            timestamp = System.currentTimeMillis()
        )

        firestoreReference.collection("MessLeaves").document(leaveId).set(leave)
            .addOnSuccessListener {
                mess.pbDismiss()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Success")
                    .setMessage(if (isException) "Emergency leave request submitted for approval." else "Leave applied successfully!")
                    .setPositiveButton("OK") { d, _ -> 
                        d.dismiss()
                        binding.tvStatus.visibility = View.GONE
                        binding.btnEmergency.visibility = View.GONE
                    }
                    .show()
            }
            .addOnFailureListener {
                mess.pbDismiss()
                mess.toast("Failed: ${it.message}")
            }
    }
}
