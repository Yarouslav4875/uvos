package com.uvos.files.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.uvos.files.data.engine.RootStorageEngine
import com.uvos.files.data.engine.SafStorageEngine
import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.data.repository.StoragePartitionRepository
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.usecase.BrowseDirectoryUseCase
import com.uvos.files.domain.usecase.FileOperationsUseCase
import com.uvos.files.ui.screens.FileBrowserScreen
import com.uvos.files.ui.screens.HexViewerScreen
import com.uvos.files.ui.screens.TextEditorScreen
import com.uvos.files.ui.theme.UvosBg
import com.uvos.files.ui.theme.UvosFilesTheme
import com.uvos.files.ui.viewmodel.FilesViewModel
import com.uvos.files.ui.viewmodel.HexEditorViewModel
import com.uvos.files.ui.viewmodel.TextEditorViewModel

sealed interface ScreenState {
    object Browser : ScreenState
    data class HexViewer(val fileItem: FileItem) : ScreenState
    data class TextEditor(val fileItem: FileItem) : ScreenState
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
        val engineRouter = StorageEngineRouter(applicationContext, safEngine, rootEngine)
        val partitionRepo = StoragePartitionRepository(applicationContext)
        val browseUseCase = BrowseDirectoryUseCase(engineRouter)
        val fileOpsUseCase = FileOperationsUseCase(engineRouter)

        viewModel = FilesViewModel(
            engineRouter = engineRouter,
            partitionRepo = partitionRepo,
            browseUseCase = browseUseCase,
            fileOpsUseCase = fileOpsUseCase
        )

        hexViewModel = HexEditorViewModel(engineRouter)
        textViewModel = TextEditorViewModel(engineRouter)

        setContent {
            UvosFilesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBg
                ) {
                    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Browser) }

                    when (val screen = currentScreen) {
                        is ScreenState.Browser -> {
                            FileBrowserScreen(
                                viewModel = viewModel,
                                onOpenFileHex = { fileItem ->
                                    hexViewModel.loadFile(fileItem)
                                    currentScreen = ScreenState.HexViewer(fileItem)
                                },
                                onOpenEditText = { fileItem ->
                                    textViewModel.loadFile(fileItem)
                                    currentScreen = ScreenState.TextEditor(fileItem)
                                }
                            )
                        }
                        is ScreenState.HexViewer -> {
                            HexViewerScreen(
                                viewModel = hexViewModel,
                                onClose = { currentScreen = ScreenState.Browser }
                            )
                        }
                        is ScreenState.TextEditor -> {
                            TextEditorScreen(
                                viewModel = textViewModel,
                                onClose = { currentScreen = ScreenState.Browser }
                            )
                        }
                    }
                }
            }
        }
    }
}
