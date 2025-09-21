package com.example.day_together.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.ZoneId

class QuestionRepository {

    private val db = Firebase.firestore


    fun loadTodayQuestion(uid: String, onResult: (String?) -> Unit) {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString() // yyyy-MM-dd

        db.collection("daily_questions")
            .whereEqualTo("uid", uid)
            .whereEqualTo("target_date", today)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val question = documents.documents[0].getString("question")
                    onResult(question)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}