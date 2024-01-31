package com.hgm.cameraxguide

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hgm.cameraxguide.ui.theme.CameraXGuideTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

      private var recording: Recording? = null

      /** 检查是否拥有权限 */
      private fun hasRequiredPermissions(): Boolean {
            return CAMERAX_PERMISSIONS.all {
                  ContextCompat.checkSelfPermission(
                        applicationContext, it
                  ) == PackageManager.PERMISSION_GRANTED
            }
      }

      /** 需要请求的权限 **/
      companion object {
            private val CAMERAX_PERMISSIONS = arrayOf(
                  Manifest.permission.CAMERA,
                  Manifest.permission.RECORD_AUDIO,
            )
      }

      override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            //请求权限
            if (!hasRequiredPermissions()) {
                  ActivityCompat.requestPermissions(
                        this, CAMERAX_PERMISSIONS, 0
                  )
            }

            setContent {
                  CameraXGuideTheme {
                        val scope = rememberCoroutineScope()
                        // 脚手架状态
                        val scaffoldState = rememberBottomSheetScaffoldState()
                        // 控制器
                        val controller = remember {
                              LifecycleCameraController(applicationContext).apply {
                                    setEnabledUseCases(
                                          //启用拍照和视频功能
                                          CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
                                    )
                              }
                        }
                        val viewModel = viewModel<MainViewModel>()
                        val bitmaps by viewModel.bitmaps.collectAsState()

                        var photoTaken by remember { mutableStateOf(false) }

                        BottomSheetScaffold(
                              scaffoldState = scaffoldState,
                              sheetPeekHeight = 0.dp,
                              sheetContent = {
                                    PhotoBottomSheet(
                                          bitmaps = bitmaps,
                                          modifier = Modifier.fillMaxWidth()
                                    )
                              }
                        ) { innerPadding ->
                              // 创建相机预览画面
                              Box(
                                    modifier = Modifier
                                          .fillMaxSize()
                                          .padding(innerPadding)
                              ) {
                                    CameraPreview(
                                          controller = controller,
                                          modifier = Modifier.fillMaxSize()
                                    )

                                    IconButton(
                                          onClick = {
                                                switchCamera(controller)
                                          },
                                          modifier = Modifier.offset(16.dp, 16.dp)
                                    ) {
                                          Icon(
                                                imageVector = Icons.Default.Cameraswitch,
                                                contentDescription = null,
                                                tint = Color.White
                                          )
                                    }


                                    Row(
                                          modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .padding(16.dp),
                                          horizontalArrangement = Arrangement.SpaceAround,
                                          verticalAlignment = Alignment.CenterVertically
                                    ) {
                                          IconButton(
                                                onClick = {
                                                      scope.launch {
                                                            scaffoldState.bottomSheetState.expand()
                                                      }
                                                },
                                          ) {
                                                Icon(
                                                      imageVector = Icons.Default.Photo,
                                                      contentDescription = null,
                                                      tint = Color.White
                                                )
                                          }

                                          IconButton(
                                                onClick = {
                                                      takePhoto(
                                                            controller = controller,
                                                            onPhotoTaken = viewModel::onTakePhoto
                                                      )
                                                      photoTaken = true
                                                },
                                          ) {
                                                Icon(
                                                      imageVector = Icons.Default.PhotoCamera,
                                                      contentDescription = null,
                                                      tint = Color.White
                                                )
                                          }

                                          IconButton(
                                                onClick = {
                                                      recordVideo(controller)
                                                },
                                          ) {
                                                Icon(
                                                      imageVector = Icons.Default.Videocam,
                                                      contentDescription = null,
                                                      tint = Color.White
                                                )
                                          }
                                    }
                              }

                              if (photoTaken) {
                                    FlashEffect{
                                          photoTaken=false
                                    }
                              }
                        }
                  }
            }
      }

      /**  切换镜头  */
      private fun switchCamera(controller: LifecycleCameraController) {
            controller.cameraSelector =
                  if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                  } else CameraSelector.DEFAULT_BACK_CAMERA
      }

      /**  拍照  */
      private fun takePhoto(
            controller: LifecycleCameraController,
            onPhotoTaken: (Bitmap) -> Unit
      ) {
            //拍照前检查权限
            if (!hasRequiredPermissions()) {
                  return
            }

            controller.takePicture(ContextCompat.getMainExecutor(applicationContext),
                  object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                              super.onCaptureSuccess(image)

                              val matrix = Matrix().apply {
                                    postRotate(image.imageInfo.rotationDegrees.toFloat())//设置旋转角度，摆正图片
                                    //postScale(-1f, 1f)//水平方向上进行-1倍缩放，相当于镜像翻转（前置镜头）
                              }
                              //创建图像
                              val bitmap = Bitmap.createBitmap(
                                    image.toBitmap(),
                                    0,
                                    0,
                                    image.width,
                                    image.height,
                                    matrix,
                                    true
                              )

                              onPhotoTaken(bitmap)
                        }

                        override fun onError(exception: ImageCaptureException) {
                              super.onError(exception)
                              Log.d("Camera", "无法进行拍照：", exception)
                        }
                  })
      }


      /**  录制视频  */
      @SuppressLint("MissingPermission")
      private fun recordVideo(
            controller: LifecycleCameraController
      ) {
            //不等于null说明已经在录制，再次点击时就结束录制保存视频
            if (recording != null) {
                  recording?.stop()
                  recording = null
                  return
            }

            //录制前检查权限
            if (!hasRequiredPermissions()) {
                  return
            }

            //创建file对象存储视频
            val outputFile = File(filesDir, "my_recording.mp4")
            recording = controller.startRecording(
                  FileOutputOptions.Builder(outputFile).build(),
                  AudioConfig.create(true),
                  ActivityCompat.getMainExecutor(applicationContext)
            ) { event ->
                  when (event) {
                        is VideoRecordEvent.Finalize -> {
                              if (event.hasError()) {
                                    recording?.close()
                                    recording = null
                                    Toast.makeText(
                                          applicationContext,
                                          "视频捕获失败",
                                          Toast.LENGTH_SHORT
                                    ).show()
                              } else {
                                    Toast.makeText(
                                          applicationContext,
                                          "视频捕获成功",
                                          Toast.LENGTH_SHORT
                                    ).show()
                              }
                        }

                        else -> {}
                  }
            }
      }
}


@Composable
fun FlashEffect(
      onFlashFinish:()->Unit
) {
      var flashEffect by remember { mutableStateOf(true) }

      LaunchedEffect(Unit) {
            delay(100)
            flashEffect = false
            onFlashFinish()
      }

      Box(
            modifier = Modifier
                  .fillMaxSize()
                  .background(if (flashEffect) Color.White else Color.Transparent)
      )
}
