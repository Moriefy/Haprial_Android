package com.haprial.app.di

import com.haprial.app.data.api.ApiClient
import com.haprial.app.data.auth.AuthStateManager
import com.haprial.app.data.db.AppDatabase
import com.haprial.app.ui.articles.ArticleListViewModel
import com.haprial.app.ui.editor.EditorViewModel
import com.haprial.app.ui.comments.CommentListViewModel
import com.haprial.app.ui.friends.FriendsViewModel
import com.haprial.app.ui.images.ImageManagerViewModel
import com.haprial.app.ui.settings.SettingsViewModel
import com.haprial.app.ui.trash.TrashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ApiClient.create(androidContext()) }
    single { AppDatabase.create(androidContext()) }
    single { get<AppDatabase>().articleDao() }
    single { AuthStateManager(androidContext()) }

    viewModel { ArticleListViewModel(get(), get()) }
    viewModel { EditorViewModel(get(), get()) }
    viewModel { CommentListViewModel(get()) }
    viewModel { FriendsViewModel(get()) }
    viewModel { ImageManagerViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { TrashViewModel(get()) }
}
