package com.dicoding.testmata.helper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateHelper {
    fun convertToFormattedDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("dd MMMM yy", Locale.ENGLISH)

        try {
            val date = inputFormat.parse(inputDate)
            return outputFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return inputDate // Return empty string if conversion fails
    }

    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yy", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }
}