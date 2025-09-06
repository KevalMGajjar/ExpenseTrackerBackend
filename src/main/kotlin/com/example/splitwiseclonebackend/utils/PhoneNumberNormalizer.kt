package com.example.splitwiseclonebackend.utils

// A simple utility object to handle phone number normalization for India.
object PhoneNumberNormalizer {
    fun normalize(phoneNumber: String): String {
        // First, remove all non-digit characters except the leading '+'
        val cleanedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        return when {
            // If it's already in the correct E.164 format for India, do nothing.
            cleanedNumber.startsWith("+91") && cleanedNumber.length == 13 -> cleanedNumber
            // If it starts with a '0', it's a local number. Remove the '0' and add +91.
            cleanedNumber.startsWith("0") && cleanedNumber.length == 11 -> "+91" + cleanedNumber.substring(1)
            // If it's a 10-digit number, it's a standard mobile number. Add +91.
            cleanedNumber.length == 10 -> "+91$cleanedNumber"
            // Otherwise, return the cleaned number as-is (might be an international number)
            else -> cleanedNumber
        }
    }
}