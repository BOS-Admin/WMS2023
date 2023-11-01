package com.bos.wms.mlkit;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class Extensions {
    public static void setImageDrawableWithAnimation(ImageView imageView,
                                                     Drawable drawable,
                                                     int duration) {
        Drawable currentDrawable = imageView.getDrawable();
        if (currentDrawable == null) {
            imageView.setImageDrawable(drawable);
            return;
        }
        imageView.setRotationX(0f);
        imageView.animate().rotation(90f).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                imageView.setImageDrawable(drawable);
                imageView.setRotation(270f);
                imageView.animate().rotation(360f).setListener(null);
            }
        });
    }

    public static String HumanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

}
