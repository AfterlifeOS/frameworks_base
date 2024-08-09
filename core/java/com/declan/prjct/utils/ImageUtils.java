/*
 * Copyright (C) 2023-2024 AfterlifeOS
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

package com.declan.prjct.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.FileDescriptor;
import java.io.IOException;

public class ImageUtils {

	public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
		if (context == null || uri == null) {
			return null;
		}
        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
		FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
		Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
		parcelFileDescriptor.close();
		return image;
    }

	public static Bitmap resizeMaxDeviceSize(Context context, Drawable image) {
		Bitmap i2b = ((BitmapDrawable) image).getBitmap();
		return resizeMaxDeviceSize(context, i2b);
    }

    public static Bitmap resizeMaxDeviceSize(Context context, Bitmap image) {
        Bitmap imageToBitmap;
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = context.getSystemService(WindowManager.class);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int maxHeight = metrics.heightPixels;
        int maxWidth = metrics.widthPixels;
        try {
            imageToBitmap = RGB565toARGB888(image);
            if (maxHeight > 0 && maxWidth > 0) {
                int width = imageToBitmap.getWidth();
                int height = imageToBitmap.getHeight();
                float ratioBitmap = (float) width / (float) height;
                float ratioMax = (float) maxWidth / (float) maxHeight;

                int finalWidth = maxWidth;
                int finalHeight = maxHeight;
                if (ratioMax > ratioBitmap) {
                    finalWidth = (int) ((float)maxHeight * ratioBitmap);
                } else {
                    finalHeight = (int) ((float)maxWidth / ratioBitmap);
                }
                imageToBitmap = Bitmap.createScaledBitmap(imageToBitmap, finalWidth, finalHeight, true);
                return imageToBitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }

	private static Bitmap RGB565toARGB888(Bitmap img) throws Exception {
        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

        //Get JPEG pixels.  Each int is the color values for one pixel.
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        //Create a Bitmap of the appropriate format.
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

        //Set RGB pixels.
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }

}