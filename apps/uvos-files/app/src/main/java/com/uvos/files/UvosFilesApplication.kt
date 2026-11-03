package com.uvos.files

import android.app.Application
import com.topjohnwu.superuser.Shell
import com.uvos.files.data.engine.RootStorageEngine
import com.uvos.files.data.engine.SafStorageEngine
import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.data.repository.StoragePartitionRepository
import com.uvos.files.domain.usecase.BrowseDirectoryUseCase
import com.uvos.files.domain.usecase.CalculateChecksumUseCase
import com.uvos.files.domain.usecase.FileOperationsUseCase

class UvosFilesApplication : Application() {

    lateinit var safEngine: SafStorageEngine
        private set
    lateinit var rootEngine: RootStorageEngine
        private set
    lateinit var engineRouter: StorageEngineRouter
        private set
    lateinit var partitionRepository: StoragePartitionRepository
        private set
    lateinit var browseUseCase: BrowseDirectoryUseCase
        private set
    lateinit var fileOpsUseCase: FileOperationsUseCase
        private set
    lateinit var checksumUseCase: CalculateChecksumUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Configure libsu Root Shell
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )

        safEngine = SafStorageEngine(this)
        rootEngine = RootStorageEngine()
        engineRouter = StorageEngineRouter(this, safEngine, rootEngine)
        partitionRepository = StoragePartitionRepository(this)
        browseUseCase = BrowseDirectoryUseCase(engineRouter)
        fileOpsUseCase = FileOperationsUseCase(engineRouter)
        checksumUseCase = CalculateChecksumUseCase()
    }

    companion object {
        lateinit var instance: UvosFilesApplication
            private set
    }
}
