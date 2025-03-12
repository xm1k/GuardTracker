package com.example.guardtracker
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
data class Task(val id: Int, val adminId: Int, val name: String, val latitude: Double, val longitude: Double, val description: String, val created: String)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme = greenDarkColorScheme) { MyApp() } }
    }
}
private val greenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2E8B57),
    onPrimary = Color.White,
    secondary = Color(0xFF2E8B57),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)
@Composable
fun MyApp() {
    val context = LocalContext.current
    var token by remember { mutableStateOf(context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getLong("token", 0L)) }
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    if (token != 0L) {
        NextScreen(token)
    } else {
        LoginScreen(
            onLoginSuccess = { tokenReceived -> prefs.edit().putLong("token", tokenReceived).apply(); token = tokenReceived },
            onLoginError = { error -> Log.e("AUTH", "Ошибка авторизации: $error") }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (Long) -> Unit = {}, onLoginError: (String) -> Unit = {}) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Guard Tracker", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 48.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.matches(Regex("^\\d*\$"))) code = it },
                label = { Text("Код доступа") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null
            )
            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        if (code.isBlank()) { errorMessage = "Введите код"; return@launch }
                        isLoading = true; errorMessage = null
                        try {
                            val response = withContext(Dispatchers.IO) { performAuthRequest(code.toInt()) }
                            Log.d("API", "Ответ сервера: $response")
                            when {
                                response.has("token") -> { val token = response.getLong("token"); onLoginSuccess(token) }
                                response.has("detail") -> { errorMessage = response.getString("detail"); onLoginError(errorMessage!!) }
                                else -> { errorMessage = "Неизвестный ответ сервера"; onLoginError(errorMessage!!) }
                            }
                        } catch (e: Exception) {
                            Log.e("API", "Ошибка соединения", e)
                            errorMessage = "Ошибка соединения: ${e.message ?: "Unknown error"}"
                            onLoginError(e.message ?: "Unknown error")
                        } finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.small,
                enabled = !isLoading
            ) { Text(text = if (isLoading) "Проверка..." else "Войти") }
        }
    }
}
private suspend fun updateAssignedTask(token: Long, task: Task, status: Int, note: String): JSONObject {
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    val url = "http://194.169.160.248:10417/update_assigned_task"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply {
        put("token", token)
        put("id", task.id)
        put("name", task.name)
        put("admin_id", task.adminId)
        put("status", status)
        put("note", note)
    }.toString()
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: "{}"
    Log.d("API", "Запрос: POST $url, Тело: $jsonBody, Код ответа: ${response.code}, Ответ: $responseBody")
    return JSONObject(responseBody)
}
private suspend fun performAuthRequest(key: Int): JSONObject {
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    val url = "http://194.169.160.248:10417/auth"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("key", key) }.toString()
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: "{}"
    Log.d("API", """
        |Запрос: POST $url
        |Тело: $jsonBody
        |Код ответа: ${response.code}
        |Ответ: $responseBody
    """.trimMargin())
    return JSONObject(responseBody)
}
@Composable
fun NextScreen(token: Long) { MapScreen(token = token) }
private suspend fun fetchTasks(token: Long): List<Task> {
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    val url = "http://194.169.160.248:10417/get_current_tasks"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("token", token) }.toString()
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: "[]"
    Log.d("API", """
        |Запрос: POST $url
        |Тело: $jsonBody
        |Код ответа: ${response.code}
        |Ответ: $responseBody
    """.trimMargin())
    val tasks = mutableListOf<Task>()
    val jsonArray = JSONArray(responseBody)
    for (i in 0 until jsonArray.length()) {
        val jsonTask = jsonArray.getJSONObject(i)
        val obj = jsonTask.getJSONObject("object")
        tasks.add(Task(id = jsonTask.getInt("id"), adminId = jsonTask.getInt("admin_id"), name = obj.getString("name"), latitude = obj.getDouble("latitude"), longitude = obj.getDouble("longitude"), description = jsonTask.getString("description"), created = jsonTask.getString("created")))
    }
    return tasks
}
@Composable
fun MapScreen(token: Long) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(emptyList<Task>()) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    LaunchedEffect(token, selectedTask) {
        while (true) {
            if (selectedTask == null) {
                try { tasks = withContext(Dispatchers.IO) { fetchTasks(token) } }
                catch (e: Exception) { Log.e("TASKS", "Ошибка получения задач", e) }
            }
            delay(1000L)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(56.3269, 44.0059))
                    setBuiltInZoomControls(false)
                    mapViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                if (selectedTask == null) {
                    mapView.overlays.clear()
                    tasks.forEach { task ->
                        val marker = Marker(mapView)
                        marker.position = GeoPoint(task.latitude, task.longitude)
                        marker.title = task.name
                        marker.setOnMarkerClickListener { _, _ -> selectedTask = task; true }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            }
        )
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { mapViewRef.value?.controller?.zoomIn() },
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(0.dp)
            ) { Text(text = "+", fontSize = 24.sp) }
            Button(
                onClick = { mapViewRef.value?.controller?.zoomOut() },
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(0.dp)
            ) { Text(text = "-", fontSize = 24.sp) }
        }
    }
    if (selectedTask != null) { TaskDetailDialog(token = token, task = selectedTask!!, onTaskUpdated = { selectedTask = null }) }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(token: Long, task: Task, onTaskUpdated: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pastelRed = Color(0xFFE57373)
    AlertDialog(
        onDismissRequest = { if (!isLoading) onTaskUpdated() },
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = task.name, style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { if (!isLoading) onTaskUpdated() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp).width(24.dp)) { Text("X") }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = task.description)
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try { withContext(Dispatchers.IO) { updateAssignedTask(token, task, 1, reason) }; onTaskUpdated() }
                            catch (e: Exception) { errorMessage = "Ошибка обновления задачи: ${e.message}" }
                            finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.small,
                    enabled = !isLoading
                ) { Text("Готово") }
                Button(
                    onClick = {
                        if (reason.isBlank()) { errorMessage = "Укажите причину для отмены задачи"; return@Button }
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try { withContext(Dispatchers.IO) { updateAssignedTask(token, task, 2, reason) }; onTaskUpdated() }
                            catch (e: Exception) { errorMessage = "Ошибка обновления задачи: ${e.message}" }
                            finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = pastelRed),
                    enabled = !isLoading
                ) { Text("Отмена") }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text(text = "Причина", color = pastelRed) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = pastelRed,
                        unfocusedBorderColor = pastelRed,
                        cursorColor = pastelRed
                    )
                )
            }
        },
        dismissButton = {}
    )
}
