// Ghép vào Activity đang quản lý WebView (APK hiện tại: com.engineering.nhatky.MainActivity).
// Không thay thế toàn bộ MainActivity và không tạo WebChromeClient thứ hai.

// Imports cần có:
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

// Fields đặt bên trong class Activity:
private var cameraFileCallback: ValueCallback<Array<Uri>>? = null
private var cameraChooserParams: WebChromeClient.FileChooserParams? = null
private var pendingCameraUri: Uri? = null

// Các launcher phải được đăng ký trước khi Activity đi vào trạng thái STARTED.
private val cameraPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        launchImageChooser(cameraGranted = granted)
    }

private val imageChooserLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val returned = result.data
        val uris: Array<Uri>? = if (result.resultCode == RESULT_OK) {
            val clip = returned?.clipData
            when {
                clip != null -> Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
                returned?.data != null -> arrayOf(returned.data!!)
                // Camera thường trả data=null khi dùng EXTRA_OUTPUT.
                pendingCameraUri != null -> arrayOf(pendingCameraUri!!)
                else -> null
            }
        } else {
            null
        }

        cameraFileCallback?.onReceiveValue(uris)
        cameraFileCallback = null
        cameraChooserParams = null
        pendingCameraUri = null
    }

private fun launchImageChooser(cameraGranted: Boolean) {
    val params = cameraChooserParams
    val captureRequested = params?.isCaptureEnabled == true

    var cameraIntent: Intent? = null
    if (cameraGranted) {
        try {
            val imageDir = File(cacheDir, "images").apply { mkdirs() }
            val photoFile = File.createTempFile("NKKT_", ".jpg", imageDir)
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                photoFile
            )
            pendingCameraUri = uri
            cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            if (cameraIntent.resolveActivity(packageManager) == null) {
                cameraIntent = null
                pendingCameraUri = null
            }
        } catch (_: Exception) {
            cameraIntent = null
            pendingCameraUri = null
        }
    }

    val launchIntent = if (captureRequested && cameraIntent != null) {
        cameraIntent!!
    } else {
        val picker = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
            )
        }
        Intent.createChooser(picker, "Chọn ảnh").also { chooser ->
            if (cameraIntent != null) {
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            }
        }
    }

    try {
        imageChooserLauncher.launch(launchIntent)
    } catch (_: Exception) {
        cameraFileCallback?.onReceiveValue(null)
        cameraFileCallback = null
        cameraChooserParams = null
        pendingCameraUri = null
    }
}

// Thêm override này vào WebChromeClient hiện có của WebView:
override fun onShowFileChooser(
    webView: android.webkit.WebView?,
    filePathCallback: ValueCallback<Array<Uri>>?,
    fileChooserParams: WebChromeClient.FileChooserParams?
): Boolean {
    if (filePathCallback == null) return false

    cameraFileCallback?.onReceiveValue(null)
    cameraFileCallback = filePathCallback
    cameraChooserParams = fileChooserParams

    val captureRequested = fileChooserParams?.isCaptureEnabled == true
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        this@MainActivity,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    if (!captureRequested || hasCameraPermission) {
        // Nếu trang không yêu cầu capture, vẫn đưa camera vào chooser khi đã được cấp quyền.
        launchImageChooser(cameraGranted = hasCameraPermission)
    } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    return true
}
