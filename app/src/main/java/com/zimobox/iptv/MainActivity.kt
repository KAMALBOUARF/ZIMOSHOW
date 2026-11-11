package com.zimobox.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zimobox.iptv.data.Channel
import com.zimobox.iptv.data.IptvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Navy = Color(0xFF071426)
private val Blue = Color(0xFF168BFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZimoApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZimoApp() {
    val repo = remember { IptvRepository() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Live TV") }
    var channels by remember { mutableStateOf(emptyList<Channel>()) }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("M3U") }
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var m3u by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("أضف قائمة M3U أو حساب Xtream للبدء") }
    var loading by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Navy,
        surface = Color(0xFF10243D), onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Navy, topBar = {
            TopAppBar(title = { Column {
                Text("ZIMO IPTV", color = Color.White)
                Text("PLAYER", color = Blue, fontSize = 11.sp)
            } }, actions = { TextButton(onClick = { showAdd = true }) { Text("+ إضافة قائمة") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy))
        }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Live TV", "Movies", "Series").forEach {
                        FilterChip(selected = tab == it, onClick = { tab = it }, label = { Text(it) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                selected?.let {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF10243D))) {
                        Column(Modifier.padding(12.dp)) {
                            Text("القناة المختارة: ${it.name}")
                            Text(it.url, fontSize = 10.sp, color = Color.LightGray)
                            Text("هذه نسخة أولية لعرض واختبار الروابط؛ مشغل الفيديو المدمج غير مضاف بعد.",
                                color = Color(0xFF9CCBFF), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(status, color = Color.LightGray, fontSize = 12.sp)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                val target = when (tab) { "Movies" -> "movies"; "Series" -> "series"; else -> "live" }
                val shown = channels.filter { it.kind == target }
                    .sortedWith(compareBy<Channel>({ it.group.lowercase() }, { it.name.lowercase() }))
                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد عناصر في هذا القسم", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(shown) { channel ->
                            Card(Modifier.fillMaxWidth().clickable { selected = channel },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10243D))) {
                                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(38.dp).background(Blue.copy(alpha = .18f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center) { Text("▶", color = Blue) }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(channel.name)
                                        Text(channel.group, color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Text("اختيار", color = Blue)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showAdd) {
            AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("إضافة قائمة") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = mode == "M3U", onClick = { mode = "M3U" }, label = { Text("M3U") })
                            FilterChip(selected = mode == "Xtream", onClick = { mode = "Xtream" }, label = { Text("Xtream") })
                        }
                        if (mode == "M3U") {
                            OutlinedTextField(m3u, { m3u = it }, label = { Text("رابط M3U") }, singleLine = true)
                        } else {
                            OutlinedTextField(host, { host = it }, label = { Text("Server URL") }, singleLine = true)
                            OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true)
                            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showAdd = false; loading = true; status = "جارٍ تحميل القائمة..."
                        scope.launch {
                            try {
                                val loaded = withContext(Dispatchers.IO) {
                                    if (mode == "M3U") repo.loadM3u(m3u.trim())
                                    else listOf("live", "movies", "series").flatMap {
                                        repo.loadXtream(host, username, password, it)
                                    }
                                }
                                channels = loaded
                                status = "تم تحميل ${loaded.size} عنصراً وترتيبها حسب المجموعة والاسم."
                            } catch (e: Exception) {
                                status = "تعذر التحميل: ${e.message ?: "تحقق من الرابط والبيانات"}"
                            } finally { loading = false }
                        }
                    }) { Text("تحميل") }
                },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("إلغاء") } })
        }
    }
}
