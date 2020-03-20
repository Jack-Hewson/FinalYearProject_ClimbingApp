/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.climbingapp;

import android.media.ImageReader.OnImageAvailableListener;
import android.util.Size;

public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    previewWidth = size.getWidth();
    previewHeight = size.getHeight();
  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connecton_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }
}