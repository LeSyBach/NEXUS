package com.example.nexus.feature_profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.ValidationUtils
import com.example.nexus.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _changePasswordState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val changePasswordState: StateFlow<Resource<Unit>> = _changePasswordState

    private val _forgotPasswordState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val forgotPasswordState: StateFlow<Resource<Unit>> = _forgotPasswordState

    val userEmail: String?
        get() = authRepository.currentUser?.email

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        if (oldPassword.isBlank()) {
            _changePasswordState.value = Resource.Error("Vui lòng nhập mật khẩu cũ")
            return
        }
        ValidationUtils.getPasswordError(newPassword)?.let {
            _changePasswordState.value = Resource.Error(it)
            return
        }
        if (newPassword != confirmPassword) {
            _changePasswordState.value = Resource.Error("Mật khẩu xác nhận không khớp")
            return
        }
        if (oldPassword == newPassword) {
            _changePasswordState.value = Resource.Error("Mật khẩu mới phải khác mật khẩu cũ")
            return
        }

        viewModelScope.launch {
            _changePasswordState.value = Resource.Loading
            try {
                authRepository.changePassword(oldPassword, newPassword)
                _changePasswordState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _changePasswordState.value = Resource.Error(
                    e.message ?: "Đổi mật khẩu thất bại"
                )
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _forgotPasswordState.value = Resource.Error("Không tìm thấy email")
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.value = Resource.Loading
            try {
                authRepository.forgotPassword(email)
                _forgotPasswordState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _forgotPasswordState.value = Resource.Error(
                    e.message ?: "Gửi email thất bại"
                )
            }
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = Resource.Idle
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = Resource.Idle
    }
}
