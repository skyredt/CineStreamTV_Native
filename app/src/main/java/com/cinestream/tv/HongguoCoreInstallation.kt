package com.cinestream.tv

import android.content.Context
import android.os.Process
import java.io.File

/** Future installation seam. No descriptor, binary or production call site is supplied in this phase. */
internal object HongguoCoreInstallation {
    /** Supply only an audited build with permitted media handling and bearer enforcement on ALL routes.
     * The unmodified upstream binary has not been established to meet this contract.
     * JNI packaging must extract libhongguotv.so into nativeLibraryDir; never copy it to writable storage.
     */
    fun runtime(context: Context, expectedVersion: String, expectedSha256: String): HongguoCoreRuntime {
        val app = context.applicationContext
        val executable = HongguoCoreRuntime.packagedExecutable(File(app.applicationInfo.nativeLibraryDir), expectedSha256)
        return HongguoCoreRuntime(File(app.noBackupFilesDir, "hongguo-core"), expectedVersion, { instance, secret, work ->
            ProcessBuilder(executable.path, "-port", "0", "-instance-id", instance,
                "-parent-pid", Process.myPid().toString()).directory(work).redirectErrorStream(true).apply {
                environment()["CINEDBKTV_CORE_TOKEN"] = secret
                environment()["HOME"] = work.path
                environment()["TMPDIR"] = work.path
                environment()["XDG_CACHE_HOME"] = File(work, "cache").path
                environment()["XDG_DATA_HOME"] = File(work, "data").path
            }.start()
        })
    }
}
