package com.servacode.directory.ios

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** The app's whole graph, made the first time the screen asks for it and kept for the process. */
private val graph: ShellGraph by lazy { ShellGraph.onDevice(ShellConfiguration.fromBundle()) }

/** The view controller SwiftUI shows (apps/ios/Daliini/DaliiniApp.swift). */
fun MainViewController(): UIViewController = ComposeUIViewController { ShellApp(graph) }
