package io.github.luannamorim.myfit.ui.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.backup.KeyState
import io.github.luannamorim.myfit.data.prefs.BackupFailure
import io.github.luannamorim.myfit.data.prefs.BackupState
import io.github.luannamorim.myfit.domain.backup.BackupFileInfo
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class SettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun settings() = compose.snapBothThemes("settings") {
        SettingsContent(SettingsUiState(system = SystemStatus(notificationsEnabled = true, batteryUnrestricted = false)), {}, {}, {}, {}, {})
    }

    @Test
    fun backup() = compose.snapBothThemes("backup") {
        val day = 1_762_970_000_000L
        BackupContent(
            state = BackupUiState(
                loading = false,
                keyState = KeyState.READY,
                backup = BackupState(folderUri = "content://x", folderName = "MyFit Backup", lastSuccessMs = day, lastError = BackupFailure.FOLDER_UNAVAILABLE),
                files = (0 until 3).map { BackupFileInfo("$it", "f$it", 2_400_000L + it * 10_000, day - it * 86_400_000L) },
            ),
            onBack = {}, onSetPassword = {}, onUnlock = {}, onChooseFolder = {}, onAutoBackup = {}, onRunNow = {}, onExport = {}, onImport = {},
            onRestoreFile = {}, onConfirmRestore = {}, onCancelRestore = {}, onMessageShown = {},
        )
    }
}
