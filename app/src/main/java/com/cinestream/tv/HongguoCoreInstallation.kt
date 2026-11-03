package com.cinestream.tv

import android.content.Context
import android.os.Process
import android.os.Build
import java.io.File

/** Pinned v0.2.138 core, launched only on demand. No protected-media route is called here. */
internal object HongguoCoreInstallation {
    const val VERSION = "0.2.138-e1af6e0cbbc018bd198169d7beb4519d7b0a271a"
    private val hashes = mapOf(
        "arm64-v8a" to "5ec423f97803cb36acedd0fb85925833cfdb37836547e4d61c82a146c7b7d100",
        "armeabi-v7a" to "88668b4399700e0a3ff7e94c7f760c40d61c67b467d9d8af40908bd35b956a74",
        "x86_64" to "f68197bfe7e9291039467b79fc15791609ccd4bd29df64802feba357f34ba2ee",
    )
    fun runtime(context: Context, onStarted: (java.lang.Process) -> Unit = {}): HongguoCoreRuntime {
        val app = context.applicationContext
        val abi = Build.SUPPORTED_ABIS.firstOrNull { it in hashes }
            ?: throw java.io.IOException("Hongguo engine does not support this Android ABI")
        val expectedSha256 = hashes.getValue(abi)
        val executable = HongguoCoreRuntime.packagedExecutable(File(app.applicationInfo.nativeLibraryDir), expectedSha256)
        val data = File(app.noBackupFilesDir, "hongguo-data").also { check(it.mkdirs() || it.isDirectory) }
        return HongguoCoreRuntime(File(app.cacheDir, "hongguo-core"), VERSION, { instance, _, work ->
            ProcessBuilder(executable.path, "-port", "0", "-instance-id", instance,
                "-parent-pid", Process.myPid().toString(), "-data-dir", data.path).directory(work).redirectErrorStream(true).apply {
                environment()["MINIREEL_CACHE_DIR"] = File(work, "media").path
                environment()["HONGGUO_TV_DATA_DIR"] = data.path
                environment()["HOME"] = work.path
                environment()["TMPDIR"] = work.path
                environment()["XDG_CACHE_HOME"] = File(work, "cache").path
                environment()["XDG_DATA_HOME"] = File(work, "data").path
            }.start().also(onStarted)
        }, startupTimeoutMs = 15000)
    }
}
