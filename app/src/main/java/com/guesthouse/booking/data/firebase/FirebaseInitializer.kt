package com.guesthouse.booking.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object FirebaseInitializer {
    fun initialize(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return false
        }
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        return true
    }

    fun isConfigured(context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty()
}
