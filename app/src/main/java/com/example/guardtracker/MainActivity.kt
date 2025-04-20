package com.example.guardtracker
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.concurrent.TimeUnit
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import android.widget.ZoomButtonsController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import java.io.IOException               // для обработки ошибок ввода‑вывода при сетевых запросах :contentReference[oaicite:0]{index=0}
import org.json.JSONException            // для обработки ошибок парсинга JSON :contentReference[oaicite:1]{index=1}
import java.lang.IllegalStateException   // при некорректном состоянии приложения :contentReference[oaicite:2]{index=2}
import androidx.compose.material3.TextField
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.DrawableCompat  // теперь должен резолвиться
import androidx.compose.ui.graphics.toArgb  // ➊
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.Color as GfxColor
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler

data class Task(val id: Int, val adminId: Int, val name: String, val latitude: Double, val longitude: Double, val description: String, val created: String)
data class TaskN(
    val id: Int,
    val userId: Int,
    val objectId: Int,
    val description: String,
    val status: Int,
    val note: String = ""
)
data class Worker(val fullname: String, val id: Int)
data class TaskItem(val description: String)

data class ObjectItem(
    val name: String,
    val description: String,
    val longitude: Double,
    val latitude: Double,
    val isDeleted: Boolean,
    val id: Int
)

suspend fun getTaskNote(token: Long, taskId: Int): String? = withContext(Dispatchers.IO) {
    val jsonBody = JSONObject()
        .put("token", token)
        .toString()
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = jsonBody.toRequestBody(mediaType)

    val client = OkHttpClient.Builder().build()
    val request = Request.Builder()
        .url("http://194.169.160.248:10417/task/get/$taskId")
        .post(body)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Ошибка сети: ${'$'}{response.code}")
        }
        val respString = response.body?.string() ?: throw IOException("Пустой ответ от сервера")
        val json = JSONObject(respString)
        json.optString("note", null)
    }
}

suspend fun fetchTasks(token: Long, status: Int = -1): List<TaskN> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/tasks"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply {
        put("token", token)
        put("status", status)
    }.toString()
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()
    val response = client.newCall(request).execute()
    val body = response.body?.string() ?: "[]"
    val arr = JSONArray(body)
    val list = mutableListOf<TaskN>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list += TaskN(
            id = o.getInt("id"),
            userId = o.getInt("user_id"),
            objectId = o.getInt("object_id"),
            description = o.getString("description"),
            status = o.getInt("status")
        )
    }
    list
}

suspend fun updateTask(token: Long, taskId: Int, status: Int): JSONObject = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/task/update"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply {
        put("token", token)
        put("task_id", taskId)
        put("status", status)
    }.toString()
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()
    val response = client.newCall(request).execute()
    return@withContext JSONObject(response.body?.string() ?: "{}")
}

suspend fun createTask(
    token: Long,
    userId: Int,
    objectId: Int,
    description: String
): Result<Boolean> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .build()
    val url = "http://194.169.160.248:10417/task/create"
    val mediaType = "application/json; charset=utf-8".toMediaType()

    // Собираем JSON-тело
    val jsonBody = JSONObject().apply {
        put("token", token)
        put("user_id", userId)
        put("object_id", objectId)
        put("description", description)
    }.toString()

    // Подготавливаем запрос
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()

    try {
        // Выполняем запрос
        Log.d("createTask", "POST $url\nBody: $jsonBody")
        val response = client.newCall(request).execute()

        val bodyString = response.body?.string().orEmpty()
        Log.d("createTask", "Response code: ${response.code}\nBody: $bodyString")

        // Если HTTP-код не в 200..299 — считаем ошибкой
        if (!response.isSuccessful) {
            return@withContext Result.failure(
                IOException("Failed to create task: HTTP ${response.code}: $bodyString")
            )
        }

        // Можно дополнительно парсить JSON, проверять поле success, если оно есть
        val json = try {
            JSONObject(bodyString)
        } catch (e: JSONException) {
            JSONObject()
        }
        val success = json.optBoolean("success", true)

        if (!success) {
            val errorMsg = json.optString("error", "Unknown error")
            return@withContext Result.failure(
                IllegalStateException("API returned failure: $errorMsg")
            )
        }

        // Всё ок — возвращаем успех
        return@withContext Result.success(true)

    } catch (e: Exception) {
        Log.e("createTask", "Exception while creating task", e)
        return@withContext Result.failure(e)
    }
}

suspend fun getWorkerName(token: Long, userId: Int): String = withContext(Dispatchers.IO) {
    // 1. Подготовка JSON-тела запроса
    val jsonBody = JSONObject()
        .put("token", token)
        .toString()
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = jsonBody.toRequestBody(mediaType)

    // 2. Формирование и выполнение HTTP‑запроса
    val client = OkHttpClient.Builder().build()
    val request = Request.Builder()
        .url("http://194.169.160.248:10417/user/get/$userId")
        .post(body)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Ошибка сети: ${response.code}")
        }
        // 3. Парсим JSON и вытаскиваем имя
        val respString = response.body?.string()
            ?: throw IOException("Пустой ответ от сервера")
        val json = JSONObject(respString)
        // полагаем, что поле с именем работника называется "fullname"
        json.optString("fullname", "Работник #$userId")
    }
}



suspend fun fetchObjects(token: Long): List<ObjectItem> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/objects"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("token", token) }.toString()
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()
    val response = client.newCall(request).execute()
    val bodyString = response.body?.string() ?: "[]"
    val jsonArray = JSONArray(bodyString)
    val list = mutableListOf<ObjectItem>()
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        list.add(
            ObjectItem(
                name = jsonObject.getString("name"),
                description = jsonObject.getString("description"),
                longitude = jsonObject.getDouble("longitude"),
                latitude = jsonObject.getDouble("latitude"),
                isDeleted = jsonObject.getBoolean("is_deleted"),
                id = jsonObject.getInt("id")
            )
        )
    }
    list
}

fun resetSession(context: Context) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().remove("token").apply()

    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

suspend fun createObject(
    token: Long,
    name: String,
    description: String,
    lat: Double,
    lon: Double
): JSONObject = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/object/create"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply {
        put("token", token)
        put("name", name)
        put("description", description)
        put("lat", lat)
        put("lon", lon)
    }.toString()
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()
    val response = client.newCall(request).execute()
    val bodyString = response.body?.string() ?: "{}"
    JSONObject(bodyString)
}

suspend fun createWorker(token: Long, fullname: String): JSONObject = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/user/create"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply {
        put("token", token)
        put("fullname", fullname)
    }.toString()
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()
    val response = client.newCall(request).execute()
    val bodyString = response.body?.string() ?: "{}"
    JSONObject(bodyString)
}

suspend fun fetchWorkers(token: Long): List<Worker> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/users"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("token", token) }.toString()
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()
    val response = client.newCall(request).execute()
    val bodyString = response.body?.string() ?: "[]"
    val jsonArray = JSONArray(bodyString)
    val list = mutableListOf<Worker>()
    for (i in 0 until jsonArray.length()){
        val jsonWorker = jsonArray.getJSONObject(i)
        // Выводим fullname и id
        list.add(Worker(jsonWorker.getString("fullname"), jsonWorker.getInt("id")))
    }
    list
}

// Новые функции для удаления
suspend fun deleteWorker(token: Long, workerId: Int): JSONObject = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/user/del/$workerId"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("token", token) }.toString()
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()
    val response = client.newCall(request).execute()
    val bodyString = response.body?.string() ?: "{}"
    JSONObject(bodyString)
}

suspend fun getObjectName(token: Long, objectId: Int): String = withContext(Dispatchers.IO) {
    // 1. Подготовка JSON-тела запроса
    val jsonBody = JSONObject()
        .put("token", token)
        .toString()                                                    // org.json.JSONObject → String :contentReference[oaicite:4]{index=4}
    val mediaType = "application/json; charset=utf-8".toMediaType()   // Расширение toMediaType() :contentReference[oaicite:5]{index=5}
    val body = jsonBody.toRequestBody(mediaType)                      // toRequestBody() для создания RequestBody :contentReference[oaicite:6]{index=6}

    // 2. Формирование и выполнение запроса
    val client = OkHttpClient.Builder().build()                       // Создание клиента OkHttp :contentReference[oaicite:7]{index=7}
    val request = Request.Builder()
        .url("http://194.169.160.248:10417/object/get/$objectId")    // URL с objectId в пути :contentReference[oaicite:8]{index=8}
        .post(body)
        .build()

    client.newCall(request).execute().use { response ->             // execute().use { … } для автоматического закрытия :contentReference[oaicite:9]{index=9}
        if (!response.isSuccessful) {
            throw IOException("Ошибка сети: ${response.code}")       // Бросаем IOException при не-2xx статусе :contentReference[oaicite:10]{index=10}
        }
        // 3. Парсим тело и вынимаем поле "name"
        val respString = response.body?.string()
            ?: throw IOException("Пустой ответ от сервера")
        val json = JSONObject(respString)                            // Снова JSONObject для разбора ответа :contentReference[oaicite:11]{index=11}
        json.optString("name", "Объект #$objectId")                  // Удобный optString с дефолтом
    }
}

suspend fun deleteObject(token: Long, objectId: Int): JSONObject = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val url = "http://194.169.160.248:10417/object/del/$objectId"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("token", token) }.toString()
    val request = Request.Builder()
        .url(url)
        .post(jsonBody.toRequestBody(mediaType))
        .build()
    val response = client.newCall(request).execute()
    val bodyString = response.body?.string() ?: "{}"
    JSONObject(bodyString)
}

suspend fun fetchTasksFromServer(token: Long, context: Context): List<Task> {
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).addInterceptor(loggingInterceptor).build()
    val url = "http://194.169.160.248:10417/tasks"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("token", token) }.toString()
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()
    val response = client.newCall(request).execute()
    if (response.code == 401) {
        resetSession(context)
        return emptyList()
    }
    val responseBody = response.body?.string() ?: "[]"
    val tasks = mutableListOf<Task>()
    val jsonArray = JSONArray(responseBody)
    for (i in 0 until jsonArray.length()){
        val jsonTask = jsonArray.getJSONObject(i)
        val obj = jsonTask.getJSONObject("object")
        tasks.add(Task(jsonTask.getInt("id"), jsonTask.getInt("admin_id"), obj.getString("name"), obj.getDouble("latitude"), obj.getDouble("longitude"), jsonTask.getString("description"), jsonTask.getString("created")))
    }
    return tasks
}
suspend fun updateAssignedTask(token: Long, task: TaskN, status: Int, note: String, context: Context): JSONObject {
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    val url = "http://194.169.160.248:10417/task/update"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply {
        put("token", token)
        put("task_id", task.id)
        put("status", status)
        put("note", note)
    }.toString()
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()
    val response = client.newCall(request).execute()
    if (response.code == 401) { // Токен недействителен
        resetSession(context)
        return JSONObject()
    }
    val responseBody = response.body?.string() ?: "{}"
    Log.d("API", "POST $url, тело: $jsonBody, код: ${response.code}, ответ: $responseBody")
    return JSONObject(responseBody)
}
// 1. Обновлённая функция performAuthRequest остаётся без изменений
suspend fun performAuthRequest(key: Int): JSONObject {
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    val url = "http://194.169.160.248:10417/auth"
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = JSONObject().apply { put("key", key) }.toString()
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: "{}"
    Log.d("API", "POST $url, тело: $jsonBody, код: ${response.code}, ответ: $responseBody")
    return JSONObject(responseBody)
}

class TaskCheckService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override fun onCreate() {
        super.onCreate()
        Log.d("TaskCheckService", "onCreate сервиса")
        createSilentNotificationChannel()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "silent_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentText("Служба запущена")
            .build()
        startForeground(1, notification)
        serviceScope.launch {
            while (isActive) {
                checkTasks()
                delay(10_000L)
            }
        }
    }
    private fun createSilentNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("silent_channel", "Фоновые операции", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Системные уведомления"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TaskCheckService", "onStartCommand вызван")
        return START_STICKY
    }

    private fun getStatusName(status: Int): String = when (status) {
        0 -> "Ожидает"
        1 -> "Сделано"
        2 -> "Отклонено"
        3 -> "В работе"
        else -> "Неизвестный статус"
    }

    // Пример: получаем имя объекта по objectId
    suspend fun getObjectName(token: Long, objectId: Int): String = withContext(Dispatchers.IO) {
        // 1. Подготовка JSON-тела запроса
        val jsonBody = JSONObject()
            .put("token", token)
            .toString()                                                    // org.json.JSONObject → String :contentReference[oaicite:4]{index=4}
        val mediaType = "application/json; charset=utf-8".toMediaType()   // Расширение toMediaType() :contentReference[oaicite:5]{index=5}
        val body = jsonBody.toRequestBody(mediaType)                      // toRequestBody() для создания RequestBody :contentReference[oaicite:6]{index=6}

        // 2. Формирование и выполнение запроса
        val client = OkHttpClient.Builder().build()                       // Создание клиента OkHttp :contentReference[oaicite:7]{index=7}
        val request = Request.Builder()
            .url("http://194.169.160.248:10417/object/get/$objectId")    // URL с objectId в пути :contentReference[oaicite:8]{index=8}
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->             // execute().use { … } для автоматического закрытия :contentReference[oaicite:9]{index=9}
            if (!response.isSuccessful) {
                throw IOException("Ошибка сети: ${response.code}")       // Бросаем IOException при не-2xx статусе :contentReference[oaicite:10]{index=10}
            }
            // 3. Парсим тело и вынимаем поле "name"
            val respString = response.body?.string()
                ?: throw IOException("Пустой ответ от сервера")
            val json = JSONObject(respString)                            // Снова JSONObject для разбора ответа :contentReference[oaicite:11]{index=11}
            json.optString("name", "Объект #$objectId")                  // Удобный optString с дефолтом
        }
    }

    private suspend fun checkTasks() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = prefs.getLong("token", 0L)
        val role = prefs.getInt("role", 0)
        if (token == 0L) return

        try {
            val tasks = fetchTasks(token, -1)

            val oldStatuses = tasks.associate { task ->
                task.id to prefs.getInt("status_${task.id}", -1)
            }

            val newTasks = tasks.filter { oldStatuses[it.id] == -1 }
            val statusChanged = tasks.filter { oldStatuses[it.id] != -1 && oldStatuses[it.id] != it.status }

            // Уведомление о смене статуса (для всех ролей)
            statusChanged.forEach { task ->
                val statusName = getStatusName(task.status)
                val objectName = getObjectName(token, task.objectId)
                showNotification(
                    title = "Задача №${task.id} — статус обновлён",
                    message = "$statusName (${task.status}) • $objectName"
                )
            }

            // Уведомление о новых задачах (только если роль != 4)
            if (role != 4) {
                newTasks.forEach { task ->
                    val objectName = getObjectName(token, task.objectId)
                    showNotification(
                        title = "Новая задача №${task.id}",
                        message = "${task.description} • $objectName"
                    )
                }
            }

            // Обновляем статусы в prefs
            prefs.edit().apply {
                tasks.forEach { task ->
                    putInt("status_${task.id}", task.status)
                }
                apply()
            }

        } catch (e: Exception) {
            Log.e("TaskCheckService", "Ошибка получения задач: ${e.message}", e)
        }
    }



    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, "task_check_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(500, 500))
            .setSound(soundUri)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("task_check_channel", "Task Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Канал для уведомлений о задачах"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        createAppNotificationChannel()
        setContent {
            MaterialTheme(colorScheme = greenDarkColorScheme) {
                MyApp()
            }
        }
    }
    private fun createAppNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("task_channel", "Task Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Уведомления о новых задачах"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
private val greenDarkColorScheme = darkColorScheme(primary = Color(0xFF2E8B57), onPrimary = Color.White, secondary = Color(0xFF2E8B57), onSecondary = Color.Black, background = Color(0xFF121212), onBackground = Color.White, surface = Color(0xFF1E1E1E), onSurface = Color.White)
@Composable
fun MyApp() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var token by remember { mutableStateOf(prefs.getLong("token", 0L)) }
    var role by remember { mutableStateOf(prefs.getInt("role", 0)) }

    LaunchedEffect(token) {
        if (token != 0L) {
            val intent = Intent(context, TaskCheckService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
    if (token != 0L) {
        NextScreen(token = token, role = role)
    } else {
        LoginScreen(
            onLoginSuccess = { tokenReceived, roleReceived ->
                prefs.edit().apply {
                    putLong("token", tokenReceived)
                    putInt("role", roleReceived)
                    apply()
                }
                token = tokenReceived
                role = roleReceived
            },
            onLoginError = { error -> Log.e("AUTH", "Ошибка авторизации: $error") }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (Long, Int) -> Unit = { _, _ -> },
    onLoginError: (String) -> Unit = {}
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sova Router",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.matches(Regex("^\\d*\$"))) code = it },
                label = { Text("Код доступа") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null
            )
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        if (code.isBlank()) {
                            errorMessage = "Введите код"
                            return@launch
                        }
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = withContext(Dispatchers.IO) { performAuthRequest(code.toInt()) }
                            Log.d("API", "Ответ сервера: $response")

                            // Проверяем наличие полей для админа: tg_id и role
                            if (response.has("tg_id") && response.has("role")) {
                                val token = response.getLong("tg_id")
                                val role = response.getInt("role")
                                // Сохраняем token и role в SharedPreferences
                                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putLong("token", token)
                                    putInt("role", role)
                                    apply()
                                }
                                onLoginSuccess(token, role)
                            }
                            // Если возвращается только token, значит это worker и role по умолчанию = 2
                            else if (response.has("token")) {
                                val token = response.getLong("token")
                                val role = 2
                                // Сохраняем token и role в SharedPreferences
                                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putLong("token", token)
                                    putInt("role", role)
                                    apply()
                                }
                                onLoginSuccess(token, role)
                            }
                            // Обрабатываем ошибку, если есть поле detail
                            else if (response.has("detail")) {
                                errorMessage = response.getString("detail")
                                onLoginError(errorMessage!!)
                            }
                            // Если формат ответа не соответствует ни одному из вариантов
                            else {
                                errorMessage = "Неизвестный ответ сервера"
                                onLoginError(errorMessage!!)
                            }
                        } catch (e: Exception) {
                            Log.e("API", "Ошибка соединения", e)
                            errorMessage = "Ошибка соединения: ${e.message ?: "Unknown error"}"
                            onLoginError(e.message ?: "Unknown error")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.small,
                enabled = !isLoading
            ) {
                Text(text = if (isLoading) "Проверка..." else "Войти")
            }
        }
    }
}



@Composable
fun NextScreen(token: Long, role: Int) {
    val activity = LocalContext.current as Activity
    var tasks by remember { mutableStateOf<List<TaskN>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Launch once to load tasks
    LaunchedEffect(Unit) {
        try {
            tasks = fetchTasks(token)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unknown error"
            showDialog = true
        }
    }

    // If error dialog is shown, intercept the back press to prevent going back
    if (showDialog) {
        BackHandler { /* consume back press */ }

        AlertDialog.Builder(activity).apply {
            setTitle("Ошибка сервера")
            setMessage("Не удалось получить данные: $error\nПриложение будет закрыто.")
            setCancelable(false)
            setPositiveButton("Закрыть") { _, _ ->
                activity.finishAffinity()
            }
        }.show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background map
        MapScreen(token = token, role = role)

        // Foreground panel
        if (role == 4) {
            AdminPanel(
                token = token,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        } else {
            TaskListPanel(
                token = token,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun AdminPanel(
    token: Long,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }  // 0=Объекты,1=Рабочие,2=Задачи
    var selectedStatusTab by remember { mutableStateOf(0) } // 0=В ожидании,1=Выполнены,2=Отклонены,3=В работе

    // Detail dialogs
    var selectedObject by remember { mutableStateOf<ObjectItem?>(null) }
    var selectedWorker by remember { mutableStateOf<Worker?>(null) }
    var selectedTask by remember { mutableStateOf<TaskN?>(null) }

    // Add dialogs flags
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Refresh triggers and lists
    var objectListRefresh by remember { mutableStateOf(0) }
    var workerListRefresh by remember { mutableStateOf(0) }
    var taskListRefresh by remember { mutableStateOf(0) }
    var statusListRefresh by remember { mutableStateOf(0) }
    var workers by remember { mutableStateOf<List<Worker>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<TaskN>>(emptyList()) }
    var objects by remember { mutableStateOf<List<ObjectItem>>(emptyList()) }

    val statusTitles = listOf("Ожидает", "В работе", "Сделано", "Отклонено")
    val statusCodes = listOf(0, 3, 1, 2)

    Surface(
        modifier = modifier,
        color = Color(0xFF212121),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Меню", tint = Color.White)
                }
                Text(
                    text = "Панель администратора",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isExpanded) {
                // Main Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF212121),
                    indicator = {}
                ) {
                    listOf("Объекты", "Рабочие", "Задачи").forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                selectedTab = idx
                                if (idx != 2) selectedStatusTab = 0
                            },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Box(
                                Modifier.fillMaxSize().background(
                                    if (selectedTab == idx) Color(0xFF424242) else Color.Transparent
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(title, color = Color.White)
                            }
                        }
                    }
                }

                when (selectedTab) {

                    // Объекты
                    0 -> {
                        // Теперь перезагружаем при изменении objectListRefresh
                        LaunchedEffect(token, objectListRefresh) {
                            objects = fetchObjects(token)
                        }
                        LazyColumn(
                            Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(objects) { obj ->
                                Card(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable { selectedObject = obj },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = obj.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${obj.description}",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                    }

                    // Рабочие
                    1 -> {
                        LaunchedEffect(token, workerListRefresh) { workers = fetchWorkers(token) }
                        Box(
                            Modifier.fillMaxWidth().weight(1f)
                        ) {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(workers) { w ->
                                    Card(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                            .clickable { selectedWorker = w },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = w.fullname,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "id: ${w.id}",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            FloatingActionButton(
                                onClick = { showAddWorkerDialog = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Добавить работника")
                            }
                        }
                    }

                    // Задачи
                    2 -> {
                        // Fetch whenever token, refresh or status changes
                        LaunchedEffect(token, taskListRefresh, statusListRefresh, selectedStatusTab) {
                            // берём из statusCodes по индексу вкладки
                            val code = statusCodes[selectedStatusTab]
                            tasks = fetchTasks(token, status = code)
                        }
                        Column(
                            Modifier.fillMaxWidth().weight(1f)
                        ) {
                            // Status sub-tabs
                            TabRow(
                                selectedTabIndex = selectedStatusTab,
                                containerColor = Color(0xFF212121),
                                indicator = {}
                            ) {
                                statusTitles.forEachIndexed { idx, title ->
                                    Tab(
                                        selected = selectedStatusTab == idx,
                                        onClick = {
                                            selectedStatusTab = idx
                                            statusListRefresh++
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Box(
                                            Modifier.fillMaxSize().background(
                                                if (selectedStatusTab == idx) Color(0xFF424242) else Color.Transparent
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        if (selectedStatusTab == idx) Color(0xFF424242)
                                                        else Color.Transparent
                                                    )
                                                    .padding(vertical = 12.dp)  // при необходимости подправить отступы
                                            )
                                        }
                                    }
                                }
                            }

                            // Task list
                            Box(Modifier.fillMaxSize()) {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(tasks) { t ->
                                        Card(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp)
                                                .clickable { selectedTask = t },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
                                        ) {
                                            Column(Modifier.padding(16.dp)) {
                                                Text("Задача #${t.id}", color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(t.description, color = Color.White)
                                                Text(
                                                    "Объект: ${t.objectId}, Работник: ${t.userId}",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Add task button
                                FloatingActionButton(
                                    onClick = { showAddTaskDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Добавить задачу")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail dialogs
    selectedObject?.let { obj ->
        ObjectDetailDialog(
            token = token,
            obj = obj,
            onDismiss = { selectedObject = null },
            onDeleteSuccess = {
                selectedObject = null
                objectListRefresh++
            }
        )
    }
    selectedWorker?.let { w ->
        WorkerDetailDialog(
            token = token,
            worker = w,
            onDismiss = { selectedWorker = null },
            onDeleteSuccess = {
                selectedWorker = null
                workerListRefresh++
            }
        )
    }

    // Add dialogs
    if (showAddWorkerDialog) {
        AddWorkerDialog(
            token = token,
            onDismiss = { showAddWorkerDialog = false },
            onCreateSuccess = {
                showAddWorkerDialog = false
                workerListRefresh++
            }
        )
    }
    if (showAddTaskDialog) {
        AddTaskDialog(
            token = token,
            onDismissRequest = { showAddTaskDialog = false },
            onCreateSuccess = {
                showAddTaskDialog = false
                taskListRefresh++
            }
        )
    }

    selectedTask?.let { t ->
        TaskUpdateDialog(
            token = token,
            task = t,
            onDismiss = { selectedTask = null },
            onUpdateSuccess = {
                selectedTask = null
                taskListRefresh++
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    token: Long,
    initialObject: ObjectItem? = null,
    onDismissRequest: () -> Unit,
    onCreateSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var objects by remember { mutableStateOf<List<ObjectItem>>(emptyList()) }
    var workers by remember { mutableStateOf<List<Worker>>(emptyList()) }
    var selectedObject by remember { mutableStateOf(initialObject) }
    var expandedObj by remember { mutableStateOf(false) }
    var selectedWorker by remember { mutableStateOf<Worker?>(null) }
    var expandedWrk by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(token) {
        objects = fetchObjects(token)
        workers = fetchWorkers(token)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Новая задача") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = expandedObj,
                    onExpandedChange = { expandedObj = it }
                ) {
                    TextField(
                        value = selectedObject?.name ?: "Выберите объект",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Объект") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedObj)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedObj,
                        onDismissRequest = { expandedObj = false }
                    ) {
                        objects.forEach { obj ->
                            DropdownMenuItem(
                                text = { Text(obj.name) },
                                onClick = {
                                    selectedObject = obj
                                    expandedObj = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedWrk,
                    onExpandedChange = { expandedWrk = it }
                ) {
                    TextField(
                        value = selectedWorker?.fullname ?: "Выберите работника",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Работник") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWrk)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedWrk,
                        onDismissRequest = { expandedWrk = false }
                    ) {
                        workers.forEach { wrk ->
                            DropdownMenuItem(
                                text = { Text(wrk.fullname) },
                                onClick = {
                                    selectedWorker = wrk
                                    expandedWrk = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (selectedObject != null && selectedWorker != null && description.isNotBlank()) {
                        createTask(
                            token,
                            userId = selectedWorker!!.id,
                            objectId = selectedObject!!.id,
                            description = description
                        )
                        onCreateSuccess()
                    }
                }
            }) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskUpdateDialog(
    token: Long,
    task: TaskN,
    onDismiss: () -> Unit,
    onUpdateSuccess: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf(task.status) }
    var menuExpanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var isLoadingNote by remember { mutableStateOf(false) }

    // New states for names
    var objectName by remember { mutableStateOf<String?>(null) }
    var isLoadingObject by remember { mutableStateOf(true) }
    var workerName by remember { mutableStateOf<String?>(null) }
    var isLoadingWorker by remember { mutableStateOf(true) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Fetch object name
    LaunchedEffect(task.objectId) {
        isLoadingObject = true
        nameError = null
        try {
            objectName = getObjectName(token, task.objectId)
        } catch (e: Exception) {
            nameError = "Ошибка загрузки имени объекта: ${e.message}"
        } finally {
            isLoadingObject = false
        }
    }

    // Fetch worker name
    LaunchedEffect(task.userId) {
        isLoadingWorker = true
        nameError = null
        try {
            workerName = getWorkerName(token, task.userId)
        } catch (e: Exception) {
            nameError = "Ошибка загрузки имени работника: ${e.message}"
        } finally {
            isLoadingWorker = false
        }
    }

    // Fetch note when status is "Отклонено"
    LaunchedEffect(selectedStatus) {
        if (selectedStatus != 2) {
            isLoadingNote = true
            note = null
            try {
                note = getTaskNote(token, task.id.toInt())
            } catch (e: Exception) {
                note = "Ошибка при загрузке причины: ${e.message}"
            } finally {
                isLoadingNote = false
            }
        }
    }

    // Status options
    val statusOptions = listOf(
        0 to "Ожидает",
        1 to "Сделано",
        2 to "Отклонено",
        3 to "В работе"
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Задача #${task.id}",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (isLoadingObject) {
                    Text("Объект: загрузка...", color = Color.Gray)
                } else {
                    Text("Объект: ${objectName.orEmpty()}", color = Color.Gray)
                }
                if (isLoadingWorker) {
                    Text("Работник: загрузка...", color = Color.Gray)
                } else {
                    Text("Работник: ${workerName.orEmpty()}", color = Color.Gray)
                }
                nameError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = task.description,
                    color = Color.White,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Text("Статус:", color = Color.White)
                ExposedDropdownMenuBox(
                    expanded = menuExpanded && !isLoading,
                    onExpandedChange = { if (!isLoading) menuExpanded = it }
                ) {
                    TextField(
                        value = statusOptions.first { it.first == selectedStatus }.second,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        statusOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedStatus = code
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Show reason if status == "Отклонено"
                if (selectedStatus != 5 && !note.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Комментарий:", color = Color.White)
                    if (isLoadingNote) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = note.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth(),
                            singleLine = false
                        )
                    }
                }

                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            withContext(Dispatchers.IO) {
                                updateTask(token, task.id, selectedStatus)
                            }
                            onUpdateSuccess()
                        } catch (e: Exception) {
                            errorMessage = "Ошибка обновления задачи: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                enabled = !isLoading
            ) {
                Text("Обновить задачу", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isLoading) onDismiss() }
            ) {
                Text("Отмена", color = Color.White)
            }
        }
    )
}

@Composable
fun ObjectDetailDialog(
    token: Long,
    obj: ObjectItem,
    onDismiss: () -> Unit,
    onDeleteSuccess: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddTask by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = obj.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                IconButton(onClick = { if (!isLoading) onDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            }
        },
        text = {
            Column {
                Text(text = "ID: ${obj.id}", color = Color.White)
                Text(text = "Описание: ${obj.description}", color = Color.White)
                Text(text = "Широта: ${obj.latitude}", color = Color.White)
                Text(text = "Долгота: ${obj.longitude}", color = Color.White)
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                deleteObject(token, obj.id)
                                onDeleteSuccess()
                            } catch (e: Exception) {
                                errorMessage = "Ошибка удаления объекта: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                    enabled = !isLoading
                ) {
                    Text("Удалить объект", color = Color.White)
                }

                Button(
                    onClick = { if (!isLoading) showAddTask = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    enabled = !isLoading
                ) {
                    Text("Создать задачу", color = Color.White)
                }
            }
        },
        dismissButton = {}
    )

    if (showAddTask) {
        AddTaskDialog(
            token = token,
            initialObject = obj,
            onDismissRequest = { showAddTask = false },
            onCreateSuccess = {
                showAddTask = false
                onDeleteSuccess()
            }
        )
    }
}

@Composable
fun WorkerDetailDialog(
    token: Long,
    worker: Worker,
    onDismiss: () -> Unit,
    onDeleteSuccess: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = worker.fullname, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                IconButton(onClick = { if (!isLoading) onDismiss() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                }
            }
        },
        text = {
            Column {
                Text(text = "ID: ${worker.id}", color = Color.White)
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val response = deleteWorker(token, worker.id)
                                // При необходимости можно проверить response и обработать его
                                onDeleteSuccess() // Например, закрыть диалог и обновить список
                            } catch (e: Exception) {
                                errorMessage = "Ошибка удаления работника: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                    enabled = !isLoading
                ) {
                    Text("Удалить работника", color = Color.White)
                }
            }
        },
        dismissButton = {}
    )
}




@Composable
fun MapScreen(
    token: Long,
    role: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Состояния
    var objects by remember { mutableStateOf(emptyList<ObjectItem>()) }
    var tasks by remember { mutableStateOf(emptyList<TaskN>()) }
    var selectedObject by remember { mutableStateOf<ObjectItem?>(null) }
    var selectedTask by remember { mutableStateOf<TaskN?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newPoint by remember { mutableStateOf<GeoPoint?>(null) }
    val mapRef = remember { mutableStateOf<MapView?>(null) }
    val scope = rememberCoroutineScope()

    // Периодические обновления
    LaunchedEffect(token, role) {
        if (role == 4) {
            while (true) {
                objects = runCatching { fetchObjects(token) }.getOrDefault(emptyList())
                delay(2_000L)
            }
        } else {
            while (true) {
                tasks = runCatching { fetchTasks(token) }.getOrDefault(emptyList())
                objects = runCatching { fetchObjects(token) }.getOrDefault(emptyList())
                delay(5_000L)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    // Настройки карты
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(57.247235, 60.095613))

                    // Скрыть стандартные контролы зума
                    setBuiltInZoomControls(false)

                    // Обработка долгого нажатия
                    overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint) = false
                        override fun longPressHelper(p: GeoPoint): Boolean {
                            newPoint = p
                            showAddDialog = true
                            return true
                        }
                    }))

                    mapRef.value = this
                }
            },
            update = { mapView ->
                // Убираем старые маркеры
                mapView.overlays
                    .filterIsInstance<Marker>()
                    .forEach { it.remove(mapView) }

                // Подготовка кастомных иконок
                val darkGreen = 0xFF2E7D32.toInt()
                val darkGray  = 0xFFFF7F50.toInt()
                fun createCircleMarker(color: Int): BitmapDrawable {
                    val size = 60
                    val strokeW = 4f
                    val radius = size / 2f - strokeW / 2
                    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        this.color = color
                    }
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        this.color = GfxColor.BLACK
                        strokeWidth = strokeW
                    }
                    canvas.drawCircle(size/2f, size/2f, radius, fillPaint)
                    canvas.drawCircle(size/2f, size/2f, radius, strokePaint)
                    return BitmapDrawable(mapView.resources, bmp)
                }
                val grayIcon = createCircleMarker(darkGray)
                val greenIcon = createCircleMarker(darkGreen)

                if (role == 4) {
                    // Админ: кликабельные объекты
                    objects.forEach { obj ->
                        Marker(mapView).apply {
                            position = GeoPoint(obj.latitude, obj.longitude)
                            setInfoWindow(null)
                            setOnMarkerClickListener { _, _ ->
                                selectedObject = obj
                                true
                            }
                        }.also { mapView.overlays.add(it) }
                    }
                } else {
                    // Пользователь: задачи
                    tasks.filter { it.status == 0 || it.status == 3 }
                        .forEach { task ->
                            objects.find { it.id == task.objectId }?.let { obj ->
                                Marker(mapView).apply {
                                    position = GeoPoint(obj.latitude, obj.longitude)
                                    setInfoWindow(null)
                                    setOnMarkerClickListener { _, _ ->
                                        selectedTask = task
                                        true
                                    }
                                    icon = if (task.status == 3) greenIcon else grayIcon
                                }.also { mapView.overlays.add(it) }
                            }
                        }
                }

                mapView.invalidate()
            }
        )

        // Свои кнопки зума
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { mapRef.value?.controller?.zoomIn() },
                modifier = Modifier.size(48.dp)
            ) { Text("+") }
            FloatingActionButton(
                onClick = { mapRef.value?.controller?.zoomOut() },
                modifier = Modifier.size(48.dp)
            ) { Text("−") }
        }
    }

    // Диалог добавления (для админа)
    if (showAddDialog && newPoint != null && role == 4) {
        AddObjectDialog(
            token = token,
            initialPosition = newPoint!!,
            onDismiss = { showAddDialog = false; newPoint = null },
            onCreated = { showAddDialog = false; newPoint = null }
        )
    }

    // Диалог деталей объекта (admin)
    selectedObject?.let { obj ->
        ObjectDetailDialog(
            token = token,
            obj = obj,
            onDismiss = { selectedObject = null },
            onDeleteSuccess = { selectedObject = null }
        )
    }

    // Диалог задачи (user)
    selectedTask?.let { task ->
        TaskDetailDialog(
            context = context,
            token = token,
            task = task,
            onTaskUpdated = { selectedTask = null }
        )
    }
}



@Composable
fun AddObjectDialog(
    token: Long,
    initialPosition: GeoPoint,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Добавить объект", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (name.isBlank()) { error = "Введите название"; return@Button }
                            isLoading = true
                            scope.launch {
                                try {
                                    // Send POST to create object
                                    withContext(Dispatchers.IO) {
                                        createObject(
                                            token = token,
                                            name = name,
                                            description = desc,
                                            lat = initialPosition.latitude,
                                            lon = initialPosition.longitude
                                        )
                                    }
                                    onCreated()
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) { Text("Создать") }
                    Button(
                        onClick = { onDismiss() },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) { Text("Отмена") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    context: Context,
    token: Long,
    task: TaskN,
    onTaskUpdated: () -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Цвета и форма кнопок
    val gray = Color(0xFF9A9A9A)
    val roundedShape = RoundedCornerShape(8.dp)

    // Название объекта
    var objectName by remember { mutableStateOf("Объект #${task.objectId}") }
    LaunchedEffect(task.objectId) {
        scope.launch {
            objectName = try {
                getObjectName(token, task.objectId)
            } catch (e: Exception) {
                "Объект #${task.objectId}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onTaskUpdated() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Задача #${task.id}",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(
                    onClick = { if (!isLoading) onTaskUpdated() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = task.description)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Объект: $objectName", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // Поле для ввода комментария
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text(text = "Комментарий") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // В работе
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                withContext(Dispatchers.IO) {
                                    updateAssignedTask(token, task, 3, comment, context)
                                }
                                onTaskUpdated()
                            } catch (e: Exception) {
                                errorMessage = "Ошибка обновления: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = roundedShape,
                    colors = ButtonDefaults.buttonColors(containerColor = gray),
                    enabled = !isLoading
                ) {
                    Text("В работе")
                }

                // В ожидании
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                withContext(Dispatchers.IO) {
                                    updateAssignedTask(token, task, 0, comment, context)
                                }
                                onTaskUpdated()
                            } catch (e: Exception) {
                                errorMessage = "Ошибка обновления: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = roundedShape,
                    colors = ButtonDefaults.buttonColors(containerColor = gray),
                    enabled = !isLoading
                ) {
                    Text("В ожидании")
                }

                // Готово (завершить)
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                withContext(Dispatchers.IO) {
                                    updateAssignedTask(token, task, 1, comment, context)
                                }
                                onTaskUpdated()
                            } catch (e: Exception) {
                                errorMessage = "Ошибка обновления: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = roundedShape,
                    enabled = !isLoading
                ) {
                    Text("Готово")
                }

                // Отмена (комментарий обязателен)
                Button(
                    onClick = {
                        if (comment.isBlank()) {
                            errorMessage = "Укажите комментарий для отмены"
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                withContext(Dispatchers.IO) {
                                    updateAssignedTask(token, task, 2, comment, context)
                                }
                                onTaskUpdated()
                            } catch (e: Exception) {
                                errorMessage = "Ошибка отмены: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = roundedShape,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Отмена")
                }
            }
        },
        dismissButton = { /* нет */ }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerDialog(
    token: Long,
    onDismiss: () -> Unit,
    onCreateSuccess: () -> Unit
) {
    var fullname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var serverKey by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        ),
        title = {
            Text(
                text = "Добавить работника",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = fullname,
                    onValueChange = { fullname = it },
                    label = { Text("ФИО") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(Modifier.height(8.dp))

                // Ошибки валидации или сети
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Показ ключа от сервера
                serverKey?.let { key ->
                    Text(
                        text = "Ваш ключ: $key",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullname.isBlank()) {
                        errorMessage = "Введите ФИО"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = withContext(Dispatchers.IO) {
                                createWorker(token, fullname)
                            }
                            // Извлечение поля ключа (убедитесь, что сервер возвращает "key")
                            val key = response.optString("key")
                            if (key.isNullOrEmpty()) {
                                errorMessage = "Не удалось получить ключ от сервера"
                            } else {
                                serverKey = key
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка создания работника: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isLoading) {
                        // Только после просмотра ключа обновляем список
                        if (!serverKey.isNullOrEmpty()) {
                            onCreateSuccess()
                        }
                        onDismiss()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Закрыть")
            }
        }
    )
}


@Composable
fun TaskListPanel(
    token: Long,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var tasks by remember { mutableStateOf<List<TaskN>>(emptyList()) }
    var taskListRefresh by remember { mutableStateOf(0) }
    var selectedTask by remember { mutableStateOf<TaskN?>(null) }

    val statusMap = listOf(0, 3)
    val titles = listOf("Ожидают", "В работе")

    val panelModifier = if (isExpanded) {
        modifier.fillMaxSize()
    } else {
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
    }

    Surface(
        modifier = panelModifier,
        color = Color(0xFF212121),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.Menu,
                        contentDescription = "Меню",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Список задач",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isExpanded) {
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF212121),
                    indicator = {}
                ) {
                    titles.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                selectedTab = idx
                                taskListRefresh++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (selectedTab == idx) Color(0xFF424242) else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(title, color = Color.White)
                            }
                        }
                    }
                }

                // Load tasks
                LaunchedEffect(token, taskListRefresh, selectedTab) {
                    tasks = fetchTasks(token, status = statusMap[selectedTab])
                }

                // Task list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedTask = task },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF424242)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Задача #${task.id}",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = task.description,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Task detail dialog
    selectedTask?.let { t ->
        TaskDetailDialog(
            context = LocalContext.current,
            token = token,
            task = t,
            onTaskUpdated = {
                selectedTask = null
                taskListRefresh++
            }
        )
    }
}
