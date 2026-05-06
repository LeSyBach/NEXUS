package com.example.nexus.feature_auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.ValidationUtils
import com.example.nexus.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val loginState: StateFlow<Resource<Unit>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val registerState: StateFlow<Resource<Unit>> = _registerState.asStateFlow()

    fun login(email: String, password: String) {
        if (!ValidationUtils.isValidEmail(email)) {
            _loginState.value = Resource.Error("Email không hợp lệ")
            return
        }
        if (password.isBlank()) {
            _loginState.value = Resource.Error("Mật khẩu không được để trống")
            return
        }

        viewModelScope.launch {
            _loginState.value = Resource.Loading
            try {
                authRepository.login(email, password)
                _loginState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _loginState.value = Resource.Error(e.message ?: "Đăng nhập thất bại. Kiểm tra lại thông tin.")
            }
        }
    }

    fun register(email: String, username: String, password: String, confirmPass: String) {
        // Validate inputs
        if (username.isBlank() || username.length < 3) {
            _registerState.value = Resource.Error("Tên hiển thị phải có ít nhất 3 ký tự")
            return
        }
        if (!ValidationUtils.isValidEmail(email)) {
            _registerState.value = Resource.Error("Email không hợp lệ")
            return
        }
        val passwordError = ValidationUtils.getPasswordError(password)
        if (passwordError != null) {
            _registerState.value = Resource.Error(passwordError)
            return
        }
        if (password != confirmPass) {
            _registerState.value = Resource.Error("Mật khẩu xác nhận không khớp")
            return
        }

        viewModelScope.launch {
            _registerState.value = Resource.Loading
            try {
                authRepository.register(email, password, username)
                _registerState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _registerState.value = Resource.Error(e.message ?: "Đăng ký thất bại. Email có thể đã tồn tại.")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = Resource.Idle
    }

    fun resetRegisterState() {
        _registerState.value = Resource.Idle
    }
}
