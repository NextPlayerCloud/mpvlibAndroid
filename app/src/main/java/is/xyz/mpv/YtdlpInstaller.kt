package `is`.xyz.mpv

import android.content.Context
import android.os.Build
import java.io.File

object YtdlpInstaller {
    private const val DIR_NAME = "mpv-ytdlp"

    fun install(context: Context): String {
        val abi = resolveAbi()
        val outDir = File(context.noBackupFilesDir, DIR_NAME)
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        val python = File(outDir, "python3")
        val pythonZip = File(outDir, "python313.zip")
        val ytdlp = File(outDir, "yt-dlp")
        val wrapper = File(outDir, "yt-dlp-wrapper")

        copyAssetIfNeeded(context, "py.$abi/python3", python)
        copyAssetIfNeeded(context, "py.$abi/python313.zip", pythonZip)
        copyAssetIfNeeded(context, "ytdl/yt-dlp", ytdlp)

        python.setExecutable(true, true)
        ytdlp.setExecutable(true, true)

        val wrapperText = """
            #!/system/bin/sh
            DIR="${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)"
            export PYTHONHOME="${'$'}DIR"
            export PYTHONPATH="${'$'}DIR/python313.zip"
            exec "${'$'}DIR/python3" "${'$'}DIR/yt-dlp" "${'$'}@"
        """.trimIndent()

        if (!wrapper.exists() || wrapper.readText() != wrapperText) {
            wrapper.writeText(wrapperText)
        }
        wrapper.setExecutable(true, true)

        return wrapper.absolutePath
    }

    private fun resolveAbi(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        return when {
            abi == "armeabi-v7a" -> "armeabi-v7a"
            abi == "arm64-v8a" -> "arm64-v8a"
            abi == "x86" -> "x86"
            abi == "x86_64" -> "x86_64"
            else -> throw IllegalStateException("Unsupported Android ABI: $abi")
        }
    }

    private fun copyAssetIfNeeded(context: Context, assetPath: String, outputFile: File) {
        if (outputFile.exists() && outputFile.length() > 0L) {
            return
        }
        outputFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
