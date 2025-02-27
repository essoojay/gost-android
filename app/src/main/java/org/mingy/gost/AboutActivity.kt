package org.mingy.gost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.mingy.gost.ui.theme.GostTheme

class AboutActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            GostTheme {
                Scaffold(topBar = {
                    TopAppBar(title = {
                        Text("${getString(R.string.gost_for_android)} - ${BuildConfig.VERSION_NAME}/${BuildConfig.GostVersion}")
                    })
                }) { contentPadding ->
                    // Screen content
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState())
                            .scrollable(orientation = Orientation.Vertical,
                                state = rememberScrollableState { delta -> 0f })
                    ) {
                        MainContent()
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MainContent() {
        val uriHandler = LocalUriHandler.current
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(buildAnnotatedString {
                append("Github: ")
                val link = LinkAnnotation.Url(
                    "https://github.com/wuzhengmao/gost-android",
                    TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary))
                ) {
                    val url = (it as LinkAnnotation.Url).url
                    uriHandler.openUri(url)
                }
                withLink(link) { append("https://github.com/wuzhengmao/gost-android") }
            })
            Text(buildAnnotatedString {
                append("Github: ")
                val link = LinkAnnotation.Url(
                    "https://github.com/go-gost/gost",
                    TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary))
                ) {
                    val url = (it as LinkAnnotation.Url).url
                    uriHandler.openUri(url)
                }
                withLink(link) { append("https://github.com/go-gost/gost") }
            })
        }
    }
}
