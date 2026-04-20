package com.example.dodgeball;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.Random;

public class Ball {
    Bitmap ball[] = new Bitmap[3];
    int ballFrame = 0;
    int ballX, ballY, ballVelocity;
    Random random;

    public Ball(Context context){
        ball[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.ball0);
        ball[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.ball1);
        ball[2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.ball2);
        random = new Random();
        resetPosition();
    }

    public Bitmap getBall(int ballFrame) {
        return ball[ballFrame];
    }

    public int getBallWidth() {
        return ball[0].getWidth();
    }

    public int getBallHeight() {
        return ball[0].getHeight();
    }

    public void resetPosition() {
        ballX = random.nextInt(GameView.dWidth - getBallWidth());
        ballY = -200 + random.nextInt(600) * -1;
        ballVelocity = 35 + random.nextInt(16);
    }
}
