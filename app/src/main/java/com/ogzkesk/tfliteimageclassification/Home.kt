package com.ogzkesk.tfliteimageclassification

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ogzkesk.tfliteimageclassification.helper.ObjectDetectorHelper
import com.ogzkesk.tfliteimageclassification.helper.PermissionUtils
import com.ogzkesk.tfliteimageclassification.util.GooeyProgressIndicator
import com.ogzkesk.tfliteimageclassification.util.Logger
import com.ogzkesk.tfliteimageclassification.util.showToast
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.detector.Detection

@Composable
fun Home() {

    val context = LocalContext.current
    val pageScrollState = rememberScrollState()
    val modelsScrollState = rememberScrollState()
    val delegatesScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var classificationResult: String by remember { mutableStateOf("") }
    var currentBitmap: Bitmap? by remember { mutableStateOf(null) }
    var inProgress: Boolean by remember { mutableStateOf(false) }
    var selectedTensorDelegate: Int by remember {
        mutableIntStateOf(ObjectDetectorHelper.DELEGATE_CPU)
    }
    var selectedTensorModel: Int by remember {
        mutableIntStateOf(ObjectDetectorHelper.MODEL_EFFICIENTNETV0)
    }

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            Logger.log(it.data)
            val intent = it.data ?: return@rememberLauncherForActivityResult
            val uri = intent.data ?: return@rememberLauncherForActivityResult

            classificationResult = ""
            currentBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            } else {
                context.contentResolver.openInputStream(uri).use { stream ->
                    BitmapFactory.decodeStream(stream).copy(Bitmap.Config.ARGB_8888, true)
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            Logger.log("permission $it")
            if (it) {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityLauncher.launch(intent)
            }
        }
    )

    val classifier = remember(selectedTensorDelegate, selectedTensorModel) {
        ObjectDetectorHelper(
            context = context,
            currentDelegate = selectedTensorDelegate,
            currentModel = selectedTensorModel,
            maxResults = 3,
            listener = object : ObjectDetectorHelper.DetectionListener {
                override fun onStart() {
                    classificationResult = ""
                    inProgress = true
                }

                override fun onError(error: String?) {
                    inProgress = false
                    context.showToast(error ?: "")
                    Logger.log(error)
                }

                override fun onResult(results: List<Detection>, inferenceTime: Long) {
                    Logger.log("Results: $results\nInferenceTime: $inferenceTime")
                    inProgress = false
                    coroutineScope.launch {
                        pageScrollState.animateScrollTo(Int.MAX_VALUE)
                    }
                    results.forEach {
                        it.categories.forEach { category ->
                            classificationResult = "$classificationResult\n\n" +
                                    "Index: ${category.index}\n" +
                                    "Display name: ${category.displayName}\n" +
                                    "Label: ${category.label}\n" +
                                    "Score: ${category.score}"
                        }
                    }
                }
            }
        )
    }

    HomeScaffold(
        inProgress = inProgress,
        currentBitmap = currentBitmap,
        classificationResult = classificationResult,
        selectedTensorModel = selectedTensorModel,
        selectedTensorDelegate = selectedTensorDelegate,
        pageScrollState = pageScrollState,
        modelsScrollState = modelsScrollState,
        delegatesScrollState = delegatesScrollState,
        onSelectedTensorModelChanged = { selectedTensorModel = it },
        onSelectedTensorDelegateChanged = { selectedTensorDelegate = it },
        onInsertClicked = {
            if (PermissionUtils.checkMediaPermission(context)) {
                try {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityLauncher.launch(galleryIntent)
                }catch (_: ActivityNotFoundException){

                }
            } else {
                try {
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }catch (_: ActivityNotFoundException){

                }
            }
        },
        onClassifyClicked = {
            if (currentBitmap == null) {
                context.showToast("Insert an image")
            } else {
                coroutineScope.launch {
                    classifier.detect(currentBitmap!!)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    inProgress: Boolean,
    currentBitmap: Bitmap?,
    classificationResult: String,
    selectedTensorDelegate: Int,
    selectedTensorModel: Int,
    modelsScrollState: ScrollState,
    delegatesScrollState: ScrollState,
    pageScrollState: ScrollState,
    onSelectedTensorDelegateChanged: (Int) -> Unit,
    onSelectedTensorModelChanged: (Int) -> Unit,
    onInsertClicked: () -> Unit,
    onClassifyClicked: () -> Unit,
) {
    Scaffold { padd ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScrollState)
                .padding(padd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Button(
                onClick = onInsertClicked,
                content = {
                    Text(text = "Insert Image")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Model:",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(modelsScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tensorModelList.forEach { model ->
                    InputChip(
                        selected = model.model == selectedTensorModel,
                        onClick = { onSelectedTensorModelChanged(model.model) },
                        label = { Text(text = model.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Delegate:",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(delegatesScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tensorDelegateList.forEach { delegate ->
                    InputChip(
                        selected = delegate.delegate == selectedTensorDelegate,
                        onClick = { onSelectedTensorDelegateChanged(delegate.delegate) },
                        label = { Text(text = delegate.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = currentBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onClassifyClicked) {
                Text(text = "Classify")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Classification Result: $classificationResult",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
        }

        if (inProgress) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GooeyProgressIndicator()
            }
        }
    }
}

data class TensorDelegate(
    val name: String,
    val delegate: Int,
)

data class TensorModel(
    val name: String,
    val model: Int,
)

val tensorModelList = listOf(
    TensorModel("EfficientNetV0", ObjectDetectorHelper.MODEL_EFFICIENTNETV0),
    TensorModel("EfficientNetV1", ObjectDetectorHelper.MODEL_EFFICIENTNETV1),
    TensorModel("EfficientNetV2", ObjectDetectorHelper.MODEL_EFFICIENTNETV2),
    TensorModel("EfficientNetV3", ObjectDetectorHelper.MODEL_EFFICIENTNETV3),
    TensorModel("EfficientNetV3x", ObjectDetectorHelper.MODEL_EFFICIENTNETV3X),
    TensorModel("EfficientNetV4", ObjectDetectorHelper.MODEL_EFFICIENTNETV4),
    TensorModel("MobileNetV1", ObjectDetectorHelper.MODEL_MOBILENETV1),
    TensorModel("MobileNetV2", ObjectDetectorHelper.MODEL_MOBILENETV2),
)

val tensorDelegateList = listOf(
    TensorDelegate("CPU", ObjectDetectorHelper.DELEGATE_CPU),
    TensorDelegate("GPU", ObjectDetectorHelper.DELEGATE_GPU),
    TensorDelegate("NNAPI", ObjectDetectorHelper.DELEGATE_NNAPI),
)

