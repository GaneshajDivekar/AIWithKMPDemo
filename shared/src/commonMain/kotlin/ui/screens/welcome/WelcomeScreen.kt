package ui.screens.welcome

import analytics.TrackScreenViewEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ebfstudio.appgpt.common.MainRes
import di.getScreenModel
import org.jetbrains.compose.resources.painterResource
import ui.components.appImagePath
import ui.screens.chat.ChatScreen

internal object WelcomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel: WelcomeScreenModel = getScreenModel()
        val uiState = screenModel.uiState

        LaunchedEffect(uiState.doNavigateToChat) {
            if (uiState.doNavigateToChat) {
                navigator.replaceAll(ChatScreen)
            }
        }

        WelcomeScreen(
            onNavigateToChatScreen = screenModel::setWelcomeShown,
        )
    }

    @Composable
    private fun WelcomeScreen(
        onNavigateToChatScreen: () -> Unit,
    ) {
        val appImage = appImagePath()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(appImage),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = MainRes.string.app_name,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = MainRes.string.welcome_title,
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = MainRes.string.welcome_subtitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateToChatScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = MainRes.string.welcome_button,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        TrackScreenViewEvent(screenName = "Welcome")
    }

}
