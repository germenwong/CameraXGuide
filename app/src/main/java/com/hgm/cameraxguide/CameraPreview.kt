package com.hgm.cameraxguide

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

/**
 * @author：HGM
 * @created：2024/1/30 0030
 * @description：相机预览组件
 **/
@Composable
fun CameraPreview(
      controller: LifecycleCameraController,
      modifier: Modifier = Modifier,
) {
      val lifecycleOwner = LocalLifecycleOwner.current

      AndroidView(
            factory = {
                  PreviewView(it).apply {
                        this.controller = controller
                        controller.bindToLifecycle(lifecycleOwner)
                  }
            },
            modifier = modifier
      )
}