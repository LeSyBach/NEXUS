package com.example.nexus.core.utils

import android.util.Patterns

/**
 * Validation utilities for user input.
 */
object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        // At least 8 chars, 1 uppercase, 1 lowercase, 1 digit
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")
        return passwordRegex.matches(password)
    }

    fun getPasswordError(password: String): String? {
        return when {
            password.isBlank() -> "Mật khẩu không được để trống"
            password.length < 8 -> "Mật khẩu phải có ít nhất 8 ký tự"
            !password.any { it.isUpperCase() } -> "Mật khẩu phải có ít nhất 1 chữ hoa"
            !password.any { it.isLowerCase() } -> "Mật khẩu phải có ít nhất 1 chữ thường"
            !password.any { it.isDigit() } -> "Mật khẩu phải có ít nhất 1 số"
            else -> null
        }
    }

    fun isValidUsername(username: String): Boolean {
        val usernameRegex = Regex("^[a-zA-Z0-9_]{${Constants.MIN_USERNAME_LENGTH},${Constants.MAX_USERNAME_LENGTH}}$")
        return usernameRegex.matches(username)
    }

    fun getUsernameError(username: String): String? {
        return when {
            username.isBlank() -> "Username không được để trống"
            username.length < Constants.MIN_USERNAME_LENGTH -> "Username phải có ít nhất ${Constants.MIN_USERNAME_LENGTH} ký tự"
            username.length > Constants.MAX_USERNAME_LENGTH -> "Username không được quá ${Constants.MAX_USERNAME_LENGTH} ký tự"
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Username chỉ chứa chữ, số và dấu gạch dưới"
            else -> null
        }
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = Regex("^(0|\\+84)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-9]|9[0-9])[0-9]{7}$")
        return phoneRegex.matches(phone.replace("\\s".toRegex(), ""))
    }

    fun getPhoneError(phone: String): String? {
        return when {
            phone.isBlank() -> null // Phone is optional
            !isValidPhoneNumber(phone) -> "Số điện thoại không hợp lệ"
            else -> null
        }
    }
}
