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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.theayushyadav11.MessEase.Models.MessLeave
import com.theayushyadav11.MessEase.R
import com.theayushyadav11.MessEase.databinding.FragmentMessLeaveBinding
import com.theayushyadav11.MessEase.ui.Adapters.MessLeaveAdapter
import com.theayushyadav11.MessEase.ui.NavigationDrawers.ViewModels.MessLeavesViewModel
import com.theayushyadav11.MessEase.utils.Constants.Companion.auth
import com.theayushyadav11.MessEase.utils.Constants.Companion.firestoreReference
import com.theayushyadav11.MessEase.utils.Mess
import java.util.*
import java.text.SimpleDateFormat

class MessLeaveFragment : Fragment() {

    private lateinit var binding: FragmentMessLeaveBinding
    private lateinit var mess: Mess
    private lateinit var viewModel: MessLeavesViewModel
    private lateinit var adapter: MessLeaveAdapter
    private val calendar = Calendar.getInstance()
    
    private var allLeaves = listOf<MessLeave>()
    private var currentFilter = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessLeaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            mess = Mess(requireContext())
            mess.log("MessLeaveFragment onViewCreated started")
            
            // Initialize ViewModel first
            viewModel = ViewModelProvider(this)[MessLeavesViewModel::class.java]
            mess.log("ViewModel created")
            
            // Setup UI components in order
            setupRecyclerView()
            mess.log("RecyclerView done")
            
            setupFilters()
            mess.log("Filters done")
            
            setupSwipeRefresh()
            mess.log("SwipeRefresh done")
            
            setupListeners()
            mess.log("Listeners done")
            
            loadLeaves()
            mess.log("Loading leaves...")
            
            mess.log("MessLeaveFragment onViewCreated completed")
        } catch (e: Exception) {
            mess.log("CRASH in MessLeaveFragment onViewCreated: ${e.message}")
            e.printStackTrace()
            try {
                Mess(requireContext()).toast("Error loading page: ${e.message}")
            } catch (ex: Exception) {
                // Can't even show toast
            }
        }
    }
    
    private fun setupRecyclerView() {
        try {
            adapter = MessLeaveAdapter(emptyList())
            binding.rvLeaves.layoutManager = LinearLayoutManager(requireContext())
            binding.rvLeaves.adapter = adapter
            mess.log("RecyclerView setup complete")
        } catch (e: Exception) {
            mess.log("Error setting up RecyclerView: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun setupFilters() {
        try {
            binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
                
                currentFilter = when (checkedIds[0]) {
                    R.id.chipAll -> "ALL"
                    R.id.chipPending -> "PENDING_APPROVAL"
                    R.id.chipApproved -> "APPROVED"
                    R.id.chipDenied -> "DENIED"
                    else -> "ALL"
                }
                
                applyFilter()
            }
            mess.log("Filters setup complete")
        } catch (e: Exception) {
            mess.log("Error setting up filters: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun setupSwipeRefresh() {
        try {
            // SwipeRefresh removed from layout for better scrolling
            mess.log("SwipeRefresh setup skipped")
        } catch (e: Exception) {
            mess.log("Error setting up SwipeRefresh: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun loadLeaves() {
        try {
            mess.log("Loading leaves...")
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                mess.log("No current user, showing empty state")
                showEmptyState(true)
                return
            }

            // Check if user is admin (can see all leaves)
            val user = mess.getUser()
            if (user.uid.isEmpty()) {
                mess.log("User UID is empty, showing empty state")
                showEmptyState(true)
                return
            }
            
            val isAdmin = user.designation == "Coordinator" || user.designation == "Developer"
            mess.log("Loading leaves for user: ${currentUser.uid}, isAdmin: $isAdmin")
            
            if (isAdmin) {
                // Admin view: Show all leaves
                viewModel.getAllLeaves { leaves ->
                    mess.log("Loaded ${leaves.size} leaves (all users)")
                    allLeaves = leaves
                    applyFilter()
                }
            } else {
                // User view: Show only their leaves
                viewModel.getUserLeaves(currentUser.uid) { leaves ->
                    mess.log("Loaded ${leaves.size} leaves (user only)")
                    allLeaves = leaves
                    applyFilter()
                }
            }
        } catch (e: Exception) {
            mess.log("Error loading leaves: ${e.message}")
            e.printStackTrace()
            showEmptyState(true)
        }
    }
    
    private fun applyFilter() {
        try {
            val filteredLeaves = when (currentFilter) {
                "ALL" -> allLeaves
                "PENDING_APPROVAL" -> allLeaves.filter { it.status == "PENDING_APPROVAL" }
                "APPROVED" -> allLeaves.filter { it.status == "APPROVED" }
                "DENIED" -> allLeaves.filter { it.status == "DENIED" || it.status == "REJECTED" }
                else -> allLeaves
            }
            
            mess.log("Applying filter: $currentFilter, showing ${filteredLeaves.size} leaves")
            adapter.updateLeaves(filteredLeaves)
            showEmptyState(filteredLeaves.isEmpty())
        } catch (e: Exception) {
            mess.log("Error applying filter: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun showEmptyState(show: Boolean) {
        try {
            if (show) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvLeaves.visibility = View.GONE
                mess.log("Showing empty state")
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvLeaves.visibility = View.VISIBLE
                mess.log("Showing leaves list")
            }
        } catch (e: Exception) {
            mess.log("Error toggling empty state: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        try {
            binding.btnFullDay.setOnClickListener {
                showDatePicker("FULL_DAY")
            }
            binding.btnMealSkip.setOnClickListener {
                showDatePicker("MEAL_SKIP")
            }
            binding.btnEmergency.setOnClickListener {
                submitEmergencyLeave()
            }
            mess.log("Button listeners setup complete")
        } catch (e: Exception) {
            mess.log("Error setting up button listeners: ${e.message}")
            e.printStackTrace()
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
                    saveLeave(type, "PENDING_APPROVAL", false)
                } else {
                    showDeadlinePassed(type)
                }
            }
            "MEAL_SKIP" -> {
                if (diffHours >= 24) {
                    saveLeave(type, "PENDING_APPROVAL", false, meal)
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
                    .setMessage(if (isException) "Emergency leave request submitted for approval." else "Leave request submitted successfully! Wait for admin approval.")
                    .setPositiveButton("OK") { d, _ -> 
                        d.dismiss()
                        binding.tvStatus.visibility = View.GONE
                        binding.btnEmergency.visibility = View.GONE
                        // Reload leaves to show the new request
                        loadLeaves()
                    }
                    .show()
            }
            .addOnFailureListener {
                mess.pbDismiss()
                mess.toast("Failed: ${it.message}")
            }
    }
}
