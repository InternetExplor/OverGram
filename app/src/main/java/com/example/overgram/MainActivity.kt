package com.example.overgram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.overgram.ui.components.ChatListItem
import com.example.overgram.ui.components.OverGramBottomBar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OverGramTheme {
                MainScreenContent()
            }
        }
    }
}

data class SampleChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

val sampleChats = listOf(
    SampleChatItem("1", "ChatGPT", "Хорошо, понял! Если будут ещё вопросы — обращайся.", "21:32", unreadCount = 3, isOnline = true),
    SampleChatItem("2", "Максим", "Давай завтра встретимся?", "20:47", unreadCount = 1),
    SampleChatItem("3", "Артём", "Скинул тебе файл", "19:23", unreadCount = 2),
    SampleChatItem("4", "Даша", "Хорошо, спасибо! 😊", "18:56", unreadCount = 1, isOnline = true),
    SampleChatItem("5", "Учеба | TUIT", "Ибрагим: Кто сделал лабу?", "17:42", unreadCount = 4),
    SampleChatItem("6", "Telegram", "Вход с нового устройства.", "16:20"),
    SampleChatItem("7", "Кирилл", "Круто, скинь потом", "14:12"),
    SampleChatItem("8", "Бобур", "Ок, вечером напишу", "12:36"),
    SampleChatItem("9", "Семья", "Мама: Будь осторожен!", "10:15")
)

@Composable
fun MainScreenContent() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            OverGramTopBar(
                title = "OverGram",
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = TextPrimary)
                    }
                }
            )
        },
        bottomBar = {
            OverGramBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {},
                    containerColor = PrimaryViolet,
                    contentColor = TextPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New message"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(sampleChats, key = { it.id }) { chat ->
                ChatListItem(
                    name = chat.name,
                    lastMessage = chat.lastMessage,
                    time = chat.time,
                    unreadCount = chat.unreadCount,
                    isOnline = chat.isOnline,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenContentPreview() {
    OverGramTheme {
        MainScreenContent()
    }
}
