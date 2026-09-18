package com.servacode.directory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.designsystem.DirectoryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var session: SessionCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DirectoryTheme {
                DirectoryApp(session.state)
            }
        }
    }
}
