package com.example.myapplication
import androidx.compose.foundation.Image
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigator(this)
        }
    }
}

@Composable
fun AppNavigator(context: Context) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("profile") { UserProfileScreen(navController, context) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Trang Chủ") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Chào mừng bạn!", fontSize = 24.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("profile") }) {
                Text("Xem hồ Sơ")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavHostController, context: Context) {

    val dataStore = context.dataStore
    val nameKey = stringPreferencesKey("user_name")
    val addressKey = stringPreferencesKey("user_address")
    val imagePathKey = stringPreferencesKey("user_image_path")

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            bitmap?.let { bmp ->
                imageBitmap = bmp
                val savedImagePath = saveImageToInternalStorage(context, bmp)
                coroutineScope.launch {
                    dataStore.edit { prefs -> prefs[imagePathKey] = savedImagePath }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val prefs = dataStore.data.first()
        name = prefs[nameKey] ?: "MinhQuan"
        address = prefs[addressKey] ?: "BINHDINH, VietNam"
        val savedImagePath = prefs[imagePathKey]
        if (savedImagePath != null) {
            imageBitmap = loadBitmapFromFile(savedImagePath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ Sơ") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            isEditing = false
                            coroutineScope.launch {
                                dataStore.edit {
                                    it[nameKey] = name
                                    it[addressKey] = address
                                }
                            }
                        }) {
                            Icon(painterResource(id = R.drawable.ic_save), contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(painterResource(id = R.drawable.ic_edit), contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            } else {
                AsyncImage(
                    model = R.drawable.profile_picture,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Địa chỉ") })
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    isEditing = false
                    coroutineScope.launch {
                        dataStore.edit {
                            it[nameKey] = name
                            it[addressKey] = address
                        }
                    }
                }) {
                    Text("Lưu")
                }
            } else {
                Text(text = name, fontSize = 20.sp, color = Color.Black)
                Text(text = address, fontSize = 16.sp, color = Color.Gray)
            }
        }
    }
}

// Hàm lưu ảnh vào bộ nhớ trong
fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String {
    val file = File(context.filesDir, "profile_image.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    return file.absolutePath
}

// Hàm tải ảnh từ file
fun loadBitmapFromFile(path: String): Bitmap? {
    return BitmapFactory.decodeFile(path)
}

// Hàm chuyển Uri thành Bitmap
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
}
