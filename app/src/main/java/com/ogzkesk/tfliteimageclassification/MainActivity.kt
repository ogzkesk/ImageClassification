package com.ogzkesk.tfliteimageclassification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ogzkesk.tfliteimageclassification.ui.theme.TfLiteImageClassificationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TfLiteImageClassificationTheme {
                Home()
            }
        }
    }
}
