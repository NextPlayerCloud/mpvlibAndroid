package `is`.xyz.mpv

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

object YTDLPManager {
    private const val TAG = "mpv-YTDLP"
    private const val YTDLP_ASSET_DIR = "ytdl"
    private const val YTDLP_BINARY = "yt-dlp"
    private const val PYTHON_DIR = "ytdl"
    private const val WRAPPER_SCRIPT = "wrapper"
    private const val SETUP_SCRIPT = "setup.py"
    private const val LAUNCHER_NAME = "youtube-dl"
    private const val DEFAULT_PYTHON_ZIP = "python313.zip"

    private var ytdlpPath: String? = null
    private var pythonPath: String? = null
    private var initialized = false

    fun prepareAssets(context: Context): Boolean {
        return try {
            val configDir = File(context.filesDir, PYTHON_DIR)
            if (!configDir.exists() && !configDir.mkdirs()) {
                Log.e(TAG, "Failed to create YTDLP dir: ${configDir.absolutePath}")
                return false
            }

            val bundledFilesReady = extractBundledFiles(context, configDir)
            if (!extractPythonBinaries(context, configDir)) {
                return false
            }
            if (!bundledFilesReady) {
                Log.e(TAG, "Neither bundled yt-dlp nor setup.py is available in assets")
                return false
            }

            val launcher = File(context.filesDir, LAUNCHER_NAME)
            writeLauncherScript(context, configDir, launcher)

            ytdlpPath = launcher.absolutePath
            pythonPath = configDir.absolutePath
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to prepare YTDLP assets", e)
            false
        }
    }

    fun initialize(context: Context): Boolean {
        if (initialized && ytdlpPath != null) return true
        if (!prepareAssets(context)) return false

        val launcher = ytdlpPath ?: return false
        MPVLib.setOptionString("ytdl", "yes")
        MPVLib.setOptionString("ytdl-path", launcher)
        initialized = true

        Log.i(TAG, "YTDLP initialized: $launcher")
        Log.i(TAG, "Python path: $pythonPath")
        return true
    }

    private fun extractBundledFiles(context: Context, destDir: File): Boolean {
        val assetManager = context.assets
        val assetNames = assetManager.list(YTDLP_ASSET_DIR)?.toSet().orEmpty()
        var hasSetup = false
        var hasBundledBinary = false

        if (SETUP_SCRIPT in assetNames) {
            copyAssetIfChanged(assetManager, "$YTDLP_ASSET_DIR/$SETUP_SCRIPT", File(destDir, SETUP_SCRIPT), executable = false)
            hasSetup = true
        }
        if (WRAPPER_SCRIPT in assetNames) {
            copyAssetIfChanged(assetManager, "$YTDLP_ASSET_DIR/$WRAPPER_SCRIPT", File(destDir, WRAPPER_SCRIPT), executable = true)
        }
        if (YTDLP_BINARY in assetNames) {
            copyAssetIfChanged(assetManager, "$YTDLP_ASSET_DIR/$YTDLP_BINARY", File(destDir, YTDLP_BINARY), executable = true)
            hasBundledBinary = true
        }

        return hasSetup || hasBundledBinary
    }

    private fun extractPythonBinaries(context: Context, destDir: File): Boolean {
        val assetManager = context.assets
        for (abi in Build.SUPPORTED_ABIS) {
            val assetDir = "py.$abi"
            val assetNames = assetManager.list(assetDir)?.toSet().orEmpty()
            if ("python3" !in assetNames) {
                continue
            }
            val stdlibZip = assetNames.firstOrNull { it.startsWith("python3") && it.endsWith(".zip") } ?: continue

            copyAssetIfChanged(assetManager, "$assetDir/python3", File(destDir, "python3"), executable = true)
            copyAssetIfChanged(assetManager, "$assetDir/$stdlibZip", File(destDir, stdlibZip), executable = false)

            destDir.listFiles()
                ?.filter { it.name.startsWith("python3") && it.name.endsWith(".zip") && it.name != stdlibZip }
                ?.forEach { it.delete() }

            Log.i(TAG, "Extracted Python runtime for ABI: $abi")
            return true
        }

        Log.e(TAG, "No Python runtime found for supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        return false
    }

    private fun writeLauncherScript(context: Context, configDir: File, launcher: File) {
        val pythonZip = resolvePythonZip(configDir)
        val launcherScript = """
            |#!/system/bin/sh
            |cd "${configDir.absolutePath}"
            |export PYTHONHOME="${configDir.absolutePath}"
            |export PYTHONPATH="${configDir.absolutePath}/$pythonZip"
            |export SSL_CERT_FILE="${context.filesDir.absolutePath}/cacert.pem"
            |if [ ! -f "${configDir.absolutePath}/$YTDLP_BINARY" ]; then
            |  if [ -f "${configDir.absolutePath}/$SETUP_SCRIPT" ]; then
            |    "${configDir.absolutePath}/python3" "${configDir.absolutePath}/$SETUP_SCRIPT" || exit $?
            |  else
            |    echo "yt-dlp is not available" >&2
            |    exit 1
            |  fi
            |fi
            |exec "${configDir.absolutePath}/python3" "${configDir.absolutePath}/$YTDLP_BINARY" "$@"
        """.trimMargin() + "\n"

        launcher.writeText(launcherScript)
        launcher.setExecutable(true)
    }

    private fun resolvePythonZip(configDir: File): String {
        return configDir.listFiles()
            ?.firstOrNull { it.name.startsWith("python3") && it.name.endsWith(".zip") }
            ?.name
            ?: DEFAULT_PYTHON_ZIP
    }

    private fun copyAssetIfChanged(
        assetManager: android.content.res.AssetManager,
        assetPath: String,
        destination: File,
        executable: Boolean,
    ) {
        destination.parentFile?.mkdirs()
        assetManager.open(assetPath).use { input ->
            val size = input.available().toLong()
            if (destination.exists() && destination.length() == size) {
                if (executable) {
                    destination.setExecutable(true)
                }
                return
            }
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (executable) {
            destination.setExecutable(true)
        }
    }

    fun getPath(): String? = ytdlpPath

    fun getPythonPath(): String? = pythonPath

    fun isAvailable(): Boolean {
        val wrapper = ytdlpPath?.let { File(it) }
        return wrapper != null && wrapper.exists() && wrapper.canExecute()
    }

    fun updateMpvOptions(ytdlFormat: String? = null) {
        ytdlpPath?.let {
            MPVLib.setOptionString("ytdl", "yes")
            MPVLib.setOptionString("ytdl-path", it)
            if (!ytdlFormat.isNullOrEmpty()) {
                MPVLib.setOptionString("ytdl-format", ytdlFormat)
            }
        }
    }
}
