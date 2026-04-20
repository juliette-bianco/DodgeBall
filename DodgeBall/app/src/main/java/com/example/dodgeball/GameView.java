package com.example.dodgeball;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {

    Bitmap background, floor, player;
    Rect rectBackground, rectFloor;
    Context context;
    Handler handler;
    final long UPDATE_MILLIS = 30;
    Runnable runnable;
    Paint textPaint = new Paint();
    Paint healthPaint = new Paint();
    float TEXT_SIZE = 120;
    int points = 0;
    int life = 3;
    static int dWidth, dHeight;
    Random random;
    float playerX, playerY;
    float oldX;
    float oldPlayerX;
    ArrayList<Ball> balls;
    ArrayList<Explosion> explosions;

    public GameView(Context context) {
        super(context);
        this.context = context;

        // Get display dimensions
        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dWidth = size.x;
        dHeight = size.y;

        // Load bitmaps with optimization
        background = decodeSampledBitmapFromResource(getResources(), R.drawable.background, dWidth, dHeight);
        floor = decodeSampledBitmapFromResource(getResources(), R.drawable.floor, dWidth, 100); // Assuming floor height
        player = decodeSampledBitmapFromResource(getResources(), R.drawable.player, 100, 100); // Assuming player size

        rectBackground = new Rect(0, 0, dWidth, dHeight);
        rectFloor = new Rect(0, dHeight - floor.getHeight(), dWidth, dHeight);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        };

        textPaint.setColor(Color.rgb(0, 0, 0));
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.LEFT);

        healthPaint.setColor(Color.GREEN);

        random = new Random();

        playerX = dWidth / 2 - player.getWidth() /2;
        playerY = dHeight - floor.getHeight() - player.getHeight();

        balls = new ArrayList<>();
        explosions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Ball ball = new Ball(context);
            balls.add(ball);
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(background, null, rectBackground, null);
        canvas.drawBitmap(floor, null, rectFloor, null);
        canvas.drawBitmap(player, playerX, playerY, null);
        for (int i = 0; i < balls.size(); i++) {
            canvas.drawBitmap(balls.get(i).getBall(balls.get(i).ballFrame), balls.get(i).ballX, balls.get(i).ballY, null);
            balls.get(i).ballFrame++;
            if (balls.get(i).ballFrame > 2) {
                balls.get(i).ballFrame = 0;
            }
            balls.get(i).ballY += balls.get(i).ballVelocity;
            if (balls.get(i).ballY + balls.get(i).getBallHeight() >= dHeight - floor.getHeight()) {
                points += 10;
                Explosion explosion = new Explosion(context);
                explosion.explosionX = balls.get(i).ballX;
                explosion.explosionY = balls.get(i).ballY;
                explosions.add(explosion);
                balls.get(i).resetPosition();
            }
        }

        for (int i = 0; i < balls.size(); i++) {
            if (balls.get(i).ballX + balls.get(i).getBallWidth() >= playerX
            && balls.get(i).ballX <= playerX + player.getWidth()
            && balls.get(i).ballY + balls.get(i).getBallWidth() >= playerY
            && balls.get(i).ballY + balls.get(i).getBallWidth() <= playerY + player.getHeight()) {
                life--;
                balls.get(i).resetPosition();
                if (life == 0) {
                    Intent intent = new Intent(context, GameOver.class);
                    intent.putExtra("points", points);
                    context.startActivity(intent);
                    ((Activity) context).finish();
                }
            }
        }

        for (int i = 0; i < explosions.size(); i++) {
            canvas.drawBitmap(explosions.get(i).getExplosion(explosions.get(i).explosionFrame), explosions.get(i).explosionX,
                    explosions.get(i).explosionY, null);
            explosions.get(i).explosionFrame++;
            if (explosions.get(i).explosionFrame > 3){
                explosions.remove(i);
            }
        }

        if (life == 2) {
            healthPaint.setColor(Color.YELLOW);
        } else if(life == 1){
            healthPaint.setColor(Color.RED);
        }
        canvas.drawRect(dWidth-200, 30, dWidth-200+60*life, 80, healthPaint);
        canvas.drawText("" + points, 20, TEXT_SIZE, textPaint);
        handler.postDelayed(runnable, UPDATE_MILLIS);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        if (touchY >= playerY) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN){
                oldX = event.getX();
                oldPlayerX = playerX;
            }
            if(action == MotionEvent.ACTION_MOVE) {
                float shift = oldX - touchX;
                float newPlayerX = oldPlayerX - shift;
                if (newPlayerX <= 0)
                    playerX = 0;
                else if(newPlayerX >= dWidth - player.getWidth())
                    playerX = dWidth - player.getWidth();
                else
                    playerX = newPlayerX;
            }
        }
        return true;
    }

    // New methods for bitmap optimization
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
