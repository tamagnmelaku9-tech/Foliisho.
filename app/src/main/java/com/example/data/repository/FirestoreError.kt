package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    LIST
}

fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    val code = if (exception is FirebaseFirestoreException) exception.code.name else "UNKNOWN"
    val message = exception.localizedMessage ?: "No message"
    Log.e(
        "FoliishoFirestore",
        """{"event":"firestore_error","operation":"${operation.name}","path":"$path","code":"$code","message":"$message"}"""
    )
}
