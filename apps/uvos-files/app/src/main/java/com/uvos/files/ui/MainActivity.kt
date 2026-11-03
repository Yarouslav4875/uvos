package com.uvos.files.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uvos.files.data.engine.FileSystemEngine
import com.uvos.files.data.engine.RootStorageEngine
import com.uvos.files.data.engine.SafStorageEngine
import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.data.repository.StoragePartitionRepository
import com.uvos.files.domain.usecase.BrowseDirectoryUseCase
import com.uvos.files.domain.usecase.CalculateChecksumUseCase
import com.uvos.files.domain.usecase.FileOperationsUseCase
import com.uvos.files.ui.screens.FileBrowserScreen
import com.uvos.files.ui.screens.HexViewerScreen
import com.uvos.files.ui.screens.TextEditorScreen
import com.uvos.files.ui.theme.UvosBackground
import com.uvos.files.ui.theme.UvosFilesTheme
import com.uvos.files.ui.viewmodel.FilesViewModel
import com.uvos.files.ui.viewmodel.HexEditorViewModel
import com.uvos.files.ui.viewmodel.TextEditorViewModel

sealed interface ScreenState {
    object Browser : ScreenState
    data class HexViewer(val path: String) : ScreenState
    data class TextEditor(val path: String) : ScreenState
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: FilesViewModel
    private lateinit var hexViewModel: HexEditorViewModel
    private lateinit var textViewModel: TextEditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация слоя движков
        val rootEngine = RootStorageEngine()
        val safEngine = SafStorageEngine(applicationContext)
        val engineRouter = StorageEngineRouter(safEngine, rootEngine)
        val partitionRepo = StoragePartitionRepository(applicationContext, engineRouter)
        val browseUseCase = BrowseDirectoryUseCase(engineRouter)
        val fileOpsUseCase = FileOperationsUseCase(engineRouter)
        val checksumUseCase = CalculateChecksumUseCase()

        viewModel = FilesViewModel(
            engineRouter = engineRouter,
            partitionRepo = partitionRepo,
            browseUseCase = browseUseCase,
            fileOpsUseCase = fileOpsUseCase
        )

        hexViewModel = HexEditorViewModel(engineRouter, checksumUseCase)
        textViewModel = TextEditorViewModel(engineRouter)

        setContent {
            UvosFilesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBackground
                ) {
                    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Browser) }

                    when (val screen = currentScreen) {
                        is ScreenState.Browser -> {
                            FileBrowserScreen(
                                viewModel = viewModel,
                                onOpenFileHex = { path ->
                                    hexViewModel.loadFile(path)
                                    currentScreen = ScreenState.HexViewer(path)
                                },
                                onOpenEditText = { path ->
                                    textViewModel.loadFile(path)
                                    currentScreen = ScreenState.TextEditor(path)
                                }
                            )
                        }
                        is ScreenState.HexViewer -> {
                            HexViewerScreen(
                                viewModel = hexViewModel,
                                onNavigateBack = { currentScreen = ScreenState.Browser }
                            )
                        }
                        is ScreenState.TextEditor -> {
                            TextEditorScreen(
                                viewModel = textViewModel,
                                onNavigateBack = { currentScreen = ScreenState.Browser }
                            )
                        }
                    }
                }
            }
        }
    }
}
