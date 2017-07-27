/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sm.Zxing.Demo.view;

import com.google.zxing.ResultPoint;
import com.sm.Zxing.Demo.camera.CameraManager;
import com.audace.Zxing.Demo.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.Collection;
import java.util.HashSet;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 100L;
  private static final int OPAQUE = 0xFF;

  private Drawable lineDrawable;//扫描线样式
  private GradientDrawable mDrawable;//扫描线渐变样式
  private Rect mRect ;
  
	/**
	 * 中间滑动线的最顶端位置
	 */
	private int slideTop;
	
	/**
	 * 中间滑动线的速度
	 */
	private int slideSpeed = 8;
	
  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int frameColor;
  private final int laserColor;
  private final int resultPointColor;
  private int scannerAlpha;
  private Collection<ResultPoint> possibleResultPoints;
  private Collection<ResultPoint> lastPossibleResultPoints;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint();
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    resultColor = resources.getColor(R.color.result_view);
    frameColor = resources.getColor(R.color.viewfinder_frame);
    laserColor = resources.getColor(R.color.viewfinder_laser);
    resultPointColor = resources.getColor(R.color.possible_result_points);
    scannerAlpha = 0;
    possibleResultPoints = new HashSet<ResultPoint>(5);
    
    //获取扫描图片 qr_scan_line.png/line_zxing
    lineDrawable = getResources().getDrawable(R.drawable.qr_scan_line);
    mRect = new Rect();
    //定义扫描线渐变样式(方向：左上到右下，颜色：黄--绿--黄)
    int [] colors = { 0xFFe9f051, 0xFFb0cb34, 0xFFe9f051 };
    mDrawable = new GradientDrawable(GradientDrawable.Orientation.BL_TR, colors);
  }

  @Override
  public void onDraw(Canvas canvas) {
	//获取扫描框
    Rect frame = CameraManager.get().getFramingRect();
    if (frame == null) {
      return;
    }
    //获取屏幕的宽和高
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);

    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(OPAQUE);
      canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
    } else {

      // Draw a two pixel solid black border inside the framing rect
      paint.setColor(frameColor);
      canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
      canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
      canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
      canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);
      
      // 画出扫描线
      // Draw a red "laser scanner" line through the middle to show decoding is active
//      paint.setColor(laserColor);
//      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
//      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
//      int middle = frame.height() / 2 + frame.top;
//      canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
        
			if (( slideTop += slideSpeed) < (frame.bottom - frame.top)) {

				// /* 以下为用渐变线条作为扫描线 */
				// // 渐变图为矩形
				// mDrawable.setShape(GradientDrawable.RECTANGLE);
				// // 渐变图为线型
				// mDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
				// // 线型矩形的四个圆角半径
				// mDrawable.setCornerRadii(new float[] { 8, 8, 8, 8, 8, 8, 8,
				// 8 });
				// // 位置边界
				// mRect.set(frame.left + 10, frame.top + i, frame.right - 10,
				// frame.top + 1 + i);
				// // 设置渐变图填充边界
				// mDrawable.setBounds(mRect);
				// // 画出渐变线条
				// mDrawable.draw(canvas);

				/* 以下为图片作为扫描线 */
				mRect.set(frame.left + 6, frame.top + slideTop, frame.right - 6,
						frame.top + 18 +slideTop );
				lineDrawable.setBounds(mRect);
				lineDrawable.draw(canvas);

			} else {
				slideTop = 0;
			}
			

   // 画出四个角
   // Draw four corners 
   paint.setColor(getResources().getColor(R.color.green));
   // 左上角(top left corner)
   canvas.drawRect(frame.left, frame.top, frame.left + 35,frame.top + 8, paint);
   canvas.drawRect(frame.left, frame.top, frame.left + 8,frame.top + 35, paint);
   // 右上角(top right corner)
   canvas.drawRect(frame.right - 35, frame.top, frame.right,frame.top + 8, paint);
   canvas.drawRect(frame.right - 8, frame.top, frame.right,frame.top + 35, paint);
   // 左下角(Bottom left corner)
   canvas.drawRect(frame.left, frame.bottom - 8, frame.left + 35,frame.bottom, paint);
   canvas.drawRect(frame.left, frame.bottom - 35, frame.left + 8,frame.bottom, paint);
   // 右下角(Bottom right corner)
   canvas.drawRect(frame.right - 35, frame.bottom - 8, frame.right,frame.bottom, paint);
   canvas.drawRect(frame.right - 8, frame.bottom - 35, frame.right,frame.bottom, paint);

//     //画捕捉的小黄点	
//      Collection<ResultPoint> currentPossible = possibleResultPoints;
//      Collection<ResultPoint> currentLast = lastPossibleResultPoints;
//      if (currentPossible.isEmpty()) {
//        lastPossibleResultPoints = null;
//      } else {
//        possibleResultPoints = new HashSet<ResultPoint>(5);
//        lastPossibleResultPoints = currentPossible;
//        paint.setAlpha(OPAQUE);
//        paint.setColor(resultPointColor);
//        for (ResultPoint point : currentPossible) {
//          canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
//        }
//      }
//      if (currentLast != null) {
//        paint.setAlpha(OPAQUE / 2);
//        paint.setColor(resultPointColor);
//        for (ResultPoint point : currentLast) {
//          canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
//        }
//      }

      // 刷新界面
      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
    }
  }

  public void drawViewfinder() {
    resultBitmap = null;
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  public void addPossibleResultPoint(ResultPoint point) {
    possibleResultPoints.add(point);
  }

}
