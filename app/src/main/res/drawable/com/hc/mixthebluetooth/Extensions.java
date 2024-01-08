package com.hc.mixthebluetooth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

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
}
