package com.sc.player.ui.add

import com.sc.player.data.LocaleHelper
import com.sc.player.R
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sc.player.data.PlaylistRepository
import com.sc.player.data.friendlyError
import com.sc.player.data.xtreamUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AddState {
    data object Idle : AddState
    data object Loading : AddState
    data class Error(val message: String) : AddState
    data class Done(val id: String) : AddState
}

class AddPlaylistViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlaylistRepository(app)
    private val text = LocaleHelper.wrap(app)
    private val _state = MutableStateFlow<AddState>(AddState.Idle)
    val state: StateFlow<AddState> = _state

    fun addM3u(name: String, url: String) = submit(name, url.trim())

    fun addXtream(name: String, server: String, user: String, pass: String) =
        submit(name, xtreamUrl(server, user, pass))

    fun clearError() {
        if (_state.value is AddState.Error) _state.value = AddState.Idle
    }

    private fun submit(name: String, url: String) {
        if (_state.value is AddState.Loading) return
        _state.value = AddState.Loading
        viewModelScope.launch {
            repo.add(name.ifBlank { text.getString(R.string.default_playlist_name) }, url).fold(
                onSuccess = { _state.value = AddState.Done(it.id) },
                onFailure = { _state.value = AddState.Error(friendlyError(text, it)) },
            )
        }
    }
}
