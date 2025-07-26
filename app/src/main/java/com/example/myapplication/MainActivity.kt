package com.example.myapplication

import android.app.ActivityOptions
import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { 
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // 查询所有可启动App
                val apps = getAllLauncherIconApps(this) //getLaunchableApps()
                // 用于刷新界面（如果需要动态刷新可用 mutableStateListOf）
                var appList by remember { mutableStateOf(apps) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        appList = appList,
                        onAppClick = { appInfo -> launchAppOnVirtualDisplay(appInfo) }
                    )
                }
            }
        }
    }

    // 获取所有带有桌面属性的App信息
    // 需要 <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
    private fun getAllLauncherIconApps(context: Context): List<AppInfo> {
        val launcherIconAppList = mutableListOf<AppInfo>()
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
        for (info in resolveInfos) {
            launcherIconAppList.add(
                AppInfo(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = info.activityInfo.packageName,
                    className = info.activityInfo.name
                )
            )
        }
        return launcherIconAppList
    }


    // 在外部显示器上启动指定App
    private fun launchAppOnVirtualDisplay(appInfo: AppInfo) {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        val externalDisplay = displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (externalDisplay != null) {
            val intent = Intent()
            intent.setClassName(appInfo.packageName, appInfo.className)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 关键：指定显示器
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = externalDisplay.displayId
            try {
                startActivity(intent, options.toBundle())
                Toast.makeText(this, "已在外部显示器上打开: ${appInfo.label}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "无法启动App: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "未检测到外部显示器", Toast.LENGTH_SHORT).show()
        }
    }

    data class AppInfo(val label: String, val packageName: String, val className: String)
}

// 新增主界面 Composable
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    appList: List<MainActivity.AppInfo>,
    onAppClick: (MainActivity.AppInfo) -> Unit
) {
    Column(modifier = modifier) {
        Text("请选择要在外部显示器打开的App：")
        LazyColumn(
            modifier = Modifier.fillMaxSize() // 让列表区域填满父布局
        ) {
            items(appList) { app ->
                Text(
                    text = app.label,
                    modifier = Modifier
                        .clickable { onAppClick(app) }
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}