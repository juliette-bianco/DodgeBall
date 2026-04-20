package com.example.dodgeball;

// Import necessary Android classes for graphics, touch, and app behavior
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

// This class represents the main game screen where everything is drawn and updated
public class GameView extends View {

    // Bitmaps (images) used in the game
    Bitmap background, floor, player;

    // Rectangles used to scale/draw images properly
    Rect rectBackground, rectFloor;

    Context context;

    // Handler is used to repeatedly update (refresh) the screen
    Handler handler;

    // Game updates every 30 milliseconds (~33 FPS)
    final long UPDATE_MILLIS = 30;

    Runnable runnable;

    // Paint objects for drawing text and health bar
    Paint textPaint = new Paint();
    Paint healthPaint = new Paint();

    float TEXT_SIZE = 120;

    // Game stats
    int points = 0;
    int life = 3;

    // Screen dimensions
    static int dWidth, dHeight;

    Random random;

    // Player position
    float playerX, playerY;

    // Used for smooth dragging movement
    float oldX;
    float oldPlayerX;

    // Lists for enemies (balls) and explosion effects
    ArrayList<Ball> balls;
    ArrayList<Explosion> explosions;

    // Constructor (runs when the game starts)
    public GameView(Context context) {
        super(context);
        this.context = context;

        // Get the size of the device screen
        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dWidth = size.x;
        dHeight = size.y;

        // Load images efficiently (scaled to fit screen)
        background = decodeSampledBitmapFromResource(getResources(), R.drawable.background, dWidth, dHeight);
        floor = decodeSampledBitmapFromResource(getResources(), R.drawable.floor, dWidth, 100);
        player = decodeSampledBitmapFromResource(getResources(), R.drawable.player, 100, 100);

        // Define where background and floor will be drawn
        rectBackground = new Rect(0, 0, dWidth, dHeight);
        rectFloor = new Rect(0, dHeight - floor.getHeight(), dWidth, dHeight);

        // Create handler for game loop
        handler = new Handler();

        // This runs repeatedly to refresh the screen
        runnable = new Runnable() {
            @Override
            public void run() {
                invalidate(); // Calls onDraw()
            }
        };

        // Set up text appearance
        textPaint.setColor(Color.rgb(0, 0, 0));
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // Set initial health bar color
        healthPaint.setColor(Color.GREEN);

        random = new Random();

        // Start player at bottom center of screen
        playerX = dWidth / 2 - player.getWidth() / 2;
        playerY = dHeight - floor.getHeight() - player.getHeight();

        // Initialize lists
        balls = new ArrayList<>();
        explosions = new ArrayList<>();

        // Create 3 balls (enemies)
        for (int i = 0; i < 3; i++) {
            Ball ball = new Ball(context);
            balls.add(ball);
        }
    }

    // This method is called repeatedly to draw everything on the screen
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background, floor, and player
        canvas.drawBitmap(background, null, rectBackground, null);
        canvas.drawBitmap(floor, null, rectFloor, null);
        canvas.drawBitmap(player, playerX, playerY, null);

        // Loop through all balls (enemies)
        for (int i = 0; i < balls.size(); i++) {

            // Draw ball animation frame
            canvas.drawBitmap(
                balls.get(i).getBall(balls.get(i).ballFrame),
                balls.get(i).ballX,
                balls.get(i).ballY,
                null
            );

            // Animate ball (cycle frames)
            balls.get(i).ballFrame++;
            if (balls.get(i).ballFrame > 2) {
                balls.get(i).ballFrame = 0;
            }

            // Move ball downward
            balls.get(i).ballY += balls.get(i).ballVelocity;

            // If ball hits the ground
            if (balls.get(i).ballY + balls.get(i).getBallHeight() >= dHeight - floor.getHeight()) {

                // Add points
                points += 10;

                // Create explosion effect
                Explosion explosion = new Explosion(context);
                explosion.explosionX = balls.get(i).ballX;
                explosion.explosionY = balls.get(i).ballY;
                explosions.add(explosion);

                // Reset ball position
                balls.get(i).resetPosition();
            }
        }

        // Check collision between player and balls
        for (int i = 0; i < balls.size(); i++) {
            if (balls.get(i).ballX + balls.get(i).getBallWidth() >= playerX
            && balls.get(i).ballX <= playerX + player.getWidth()
            && balls.get(i).ballY + balls.get(i).getBallWidth() >= playerY
            && balls.get(i).ballY + balls.get(i).getBallWidth() <= playerY + player.getHeight()) {

                // Player gets hit > lose life
                life--;

                // Reset ball
                balls.get(i).resetPosition();

                // If no lives left > go to Game Over screen
                if (life == 0) {
                    Intent intent = new Intent(context, GameOver.class);
                    intent.putExtra("points", points);
                    context.startActivity(intent);
                    ((Activity) context).finish();
                }
            }
        }

        // Draw explosion animations
        for (int i = 0; i < explosions.size(); i++) {
            canvas.drawBitmap(
                explosions.get(i).getExplosion(explosions.get(i).explosionFrame),
                explosions.get(i).explosionX,
                explosions.get(i).explosionY,
                null
            );

            // Animate explosion
            explosions.get(i).explosionFrame++;

            // Remove explosion after animation finishes
            if (explosions.get(i).explosionFrame > 3){
                explosions.remove(i);
            }
        }

        // Change health bar color based on remaining lives
        if (life == 2) {
            healthPaint.setColor(Color.YELLOW);
        } else if (life == 1) {
            healthPaint.setColor(Color.RED);
        }

        // Draw health bar
        canvas.drawRect(dWidth - 200, 30, dWidth - 200 + 60 * life, 80, healthPaint);

        // Draw score
        canvas.drawText("" + points, 20, TEXT_SIZE, textPaint);

        // Schedule next frame update
        handler.postDelayed(runnable, UPDATE_MILLIS);
    }

    // Handles player touch input (dragging left/right)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float touchX = event.getX();
        float touchY = event.getY();

        // Only allow movement if touch is near player
        if (touchY >= playerY) {

            int action = event.getAction();

            // When user first touches screen
            if (action == MotionEvent.ACTION_DOWN){
                oldX = event.getX();
                oldPlayerX = playerX;
            }

            // When user drags finger
            if (action == MotionEvent.ACTION_MOVE) {

                float shift = oldX - touchX;
                float newPlayerX = oldPlayerX - shift;

                // Keep player inside screen bounds
                if (newPlayerX <= 0)
                    playerX = 0;
                else if (newPlayerX >= dWidth - player.getWidth())
                    playerX = dWidth - player.getWidth();
                else
                    playerX = newPlayerX;
            }
        }
        return true;
    }

    /**
     * Efficiently loads a scaled bitmap to reduce memory usage
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();

        // Only get image size first (no memory allocation yet)
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate how much to scale image down
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode actual bitmap with scaling applied
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Calculates the best scaling factor for an image
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;

        int inSampleSize = 1;

        // Reduce size until it fits requested dimensions
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
