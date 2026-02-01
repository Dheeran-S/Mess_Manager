package com.theayushyadav11.MessEase.ui.NavigationDrawers.ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.theayushyadav11.MessEase.Models.MessLeave
import com.theayushyadav11.MessEase.utils.Constants.Companion.firestoreReference

class MessLeavesViewModel : ViewModel() {
    
    private val TAG = "MessLeavesViewModel"
    
    /**
     * Get all leave requests for a specific user
     */
    fun getUserLeaves(uid: String, onResult: (List<MessLeave>) -> Unit) {
        try {
            firestoreReference.collection("MessLeaves")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching user leaves: ${error.message}")
                        error.printStackTrace()
                        onResult(emptyList())
                        return@addSnapshotListener
                    }
                    
                    try {
                        val leaves = mutableListOf<MessLeave>()
                        snapshot?.documents?.forEach { doc ->
                            try {
                                doc.toObject(MessLeave::class.java)?.let { leave ->
                                    leaves.add(leave)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing leave document: ${e.message}")
                            }
                        }
                        // Sort by timestamp in code (descending - newest first)
                        leaves.sortByDescending { it.timestamp }
                        Log.d(TAG, "Fetched ${leaves.size} leaves for user $uid")
                        onResult(leaves)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing leaves: ${e.message}")
                        e.printStackTrace()
                        onResult(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getUserLeaves: ${e.message}")
            e.printStackTrace()
            onResult(emptyList())
        }
    }
    
    /**
     * Get all leave requests (for admin view)
     */
    fun getAllLeaves(onResult: (List<MessLeave>) -> Unit) {
        try {
            firestoreReference.collection("MessLeaves")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching all leaves: ${error.message}")
                        error.printStackTrace()
                        onResult(emptyList())
                        return@addSnapshotListener
                    }
                    
                    try {
                        val leaves = mutableListOf<MessLeave>()
                        snapshot?.documents?.forEach { doc ->
                            try {
                                doc.toObject(MessLeave::class.java)?.let { leave ->
                                    leaves.add(leave)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing leave document: ${e.message}")
                            }
                        }
                        // Sort by timestamp in code (descending - newest first)
                        leaves.sortByDescending { it.timestamp }
                        Log.d(TAG, "Fetched ${leaves.size} total leaves")
                        onResult(leaves)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing leaves: ${e.message}")
                        e.printStackTrace()
                        onResult(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAllLeaves: ${e.message}")
            e.printStackTrace()
            onResult(emptyList())
        }
    }
    
    /**
     * Get pending leaves count for a user
     */
    fun getPendingLeavesCount(uid: String, onResult: (Int) -> Unit) {
        firestoreReference.collection("MessLeaves")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", "PENDING_APPROVAL")
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.size())
            }
            .addOnFailureListener {
                onResult(0)
            }
    }
    
    /**
     * Get user's latest leave status
     */
    fun getLatestLeave(uid: String, onResult: (MessLeave?) -> Unit) {
        firestoreReference.collection("MessLeaves")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val leave = snapshot.documents.firstOrNull()?.toObject(MessLeave::class.java)
                onResult(leave)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
