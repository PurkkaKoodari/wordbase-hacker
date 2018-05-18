package net.pietu1998.wordbasehacker;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.support.v4.math.MathUtils;
import android.view.MotionEvent;
import android.view.View;

@SuppressLint("ViewConstructor")
public class HudView extends View {
    private final HudService service;

    public HudView(HudService context, int defaultWidth) {
        super(context);
        this.service = context;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        x = preferences.getInt(context.getString(R.string.pref_key_hudx), 0);
        y = preferences.getInt(context.getString(R.string.pref_key_hudy), 0);
        width = preferences.getInt(context.getString(R.string.pref_key_hudwidth), defaultWidth);
        setOnTouchListener((view, event) -> {
            int screenWidth = getWidth();
            int screenHeight = getHeight();
            int buttonSize = screenWidth / 10;
            RectF moveButton = new RectF(screenWidth / 2 - buttonSize, screenHeight / 2 - buttonSize,
                    screenWidth / 2 + buttonSize, screenHeight / 2 + buttonSize);
            RectF resizeButton = new RectF(0, screenHeight / 2 - buttonSize,
                    buttonSize * 2, screenHeight / 2 + buttonSize);
            RectF acceptButton = new RectF(screenWidth - buttonSize * 2, screenHeight / 2 - buttonSize,
                    screenWidth, screenHeight / 2 + buttonSize);
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (moving || resizing || accepting)
                        break;
                    if (moveButton.contains(event.getX(), event.getY()))
                        moving = true;
                    else if (resizeButton.contains(event.getX(), event.getY()))
                        resizing = true;
                    else if (acceptButton.contains(event.getX(), event.getY()))
                        accepting = true;
                    startX = event.getX();
                    startY = event.getY();
                    origX = x;
                    origY = y;
                    origWidth = width;
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_MOVE:
                    if (moving) {
                        x = MathUtils.clamp(origX + (int) ((event.getX() - startX) / 4), 0, Math.max(0, screenWidth - width));
                        y = MathUtils.clamp(origY + (int) ((event.getY() - startY) / 4), 0, Math.max(0, (int) (screenHeight - 1.3f * width)));
                    } else if (resizing) {
                        width = MathUtils.clamp(origWidth + (int) ((startY - event.getY()) / 4), 50, Math.max(50, screenWidth - x));
                    } else if (accepting && !acceptButton.contains(event.getX(), event.getY())) {
                        accepting = false;
                    }
                    if (action == MotionEvent.ACTION_UP) {
                        if (accepting) {
                            PreferenceManager.getDefaultSharedPreferences(service).edit()
                                    .putInt(service.getString(R.string.pref_key_hudx), x)
                                    .putInt(service.getString(R.string.pref_key_hudy), y)
                                    .putInt(service.getString(R.string.pref_key_hudwidth), width)
                                    .apply();
                            service.hudOperationDone();
                        }
                        moving = resizing = accepting = false;
                    }
                    invalidate();
                    break;
            }
            return true;
        });
    }

    private boolean editMode = false;
    private int x, y, width;

    private byte[] coordinates;

    private boolean moving = false;
    private boolean resizing = false;
    private boolean accepting = false;
    private int origX, origY, origWidth;
    private float startX, startY;

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void setCoordinates(byte[] coordinates) {
        this.coordinates = coordinates;
    }

    private static final float STROKE = 1;
    private static final float CORNER = 5;

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (editMode) {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(width / 100f, width / 100f);
            Paint backPaint = new Paint();
            backPaint.setColor(0xFF009900);
            backPaint.setStrokeWidth(2 * STROKE);
            backPaint.setStyle(Paint.Style.STROKE);
            backPaint.setStrokeCap(Paint.Cap.SQUARE);
            canvas.drawLine(STROKE, STROKE, CORNER, STROKE, backPaint);
            canvas.drawLine(STROKE, STROKE, STROKE, CORNER, backPaint);
            canvas.drawLine(100 - CORNER, STROKE, 100 - STROKE, STROKE, backPaint);
            canvas.drawLine(100 - STROKE, STROKE, 100 - STROKE, CORNER, backPaint);
            canvas.drawLine(100 - CORNER, 130 - STROKE, 100 - STROKE, 130 - STROKE, backPaint);
            canvas.drawLine(100 - STROKE, 130 - CORNER, 100 - STROKE, 130 - STROKE, backPaint);
            canvas.drawLine(STROKE, 130 - STROKE, 5, 130 - STROKE, backPaint);
            canvas.drawLine(STROKE, 130 - CORNER, 1, 130 - STROKE, backPaint);
            canvas.restore();
            int width = getWidth();
            int height = getHeight();
            int buttonSize = width / 5;
            Paint buttonPaint = new Paint();
            buttonPaint.setColor(moving ? 0xC000FF00 : 0xC0FFFF00);
            buttonPaint.setStyle(Paint.Style.FILL);
            Paint buttonFgPaint = new Paint();
            buttonFgPaint.setColor(0xC0FFFFFF);
            buttonFgPaint.setStyle(Paint.Style.FILL);
            canvas.save();
            canvas.translate(width / 2, height / 2);
            canvas.scale(buttonSize / 100f, buttonSize / 100f);
            canvas.drawRect(-50f, -50f, 50f, 50f, buttonPaint);
            Path moveArrow = new Path();
            moveArrow.moveTo(0f, -40f);
            moveArrow.lineTo(15f, -25f);
            moveArrow.lineTo(5f, -25f);
            moveArrow.lineTo(5f, -5f);
            moveArrow.lineTo(0f, 0f);
            moveArrow.lineTo(-5f, -5f);
            moveArrow.lineTo(-5f, -25f);
            moveArrow.lineTo(-15f, -25f);
            moveArrow.close();
            for (int i = 0; i < 4; i++) {
                canvas.drawPath(moveArrow, buttonFgPaint);
                canvas.rotate(90, 0, 0);
            }
            canvas.restore();
            canvas.save();
            canvas.translate(buttonSize / 2, height / 2);
            canvas.scale(buttonSize / 100f, buttonSize / 100f);
            buttonPaint.setColor(resizing ? 0xC000FF00 : 0xC0FFFF00);
            canvas.drawRect(-50f, -50f, 50f, 50f, buttonPaint);
            Path resizeArrow = new Path();
            resizeArrow.moveTo(0f, -40f);
            resizeArrow.lineTo(25f, -15f);
            resizeArrow.lineTo(10f, -15f);
            resizeArrow.lineTo(5f, 25f);
            resizeArrow.lineTo(15f, 25f);
            resizeArrow.lineTo(0f, 40f);
            resizeArrow.lineTo(-15f, 25f);
            resizeArrow.lineTo(-5f, 25f);
            resizeArrow.lineTo(-10f, -15f);
            resizeArrow.lineTo(-25f, -15f);
            resizeArrow.close();
            canvas.drawPath(resizeArrow, buttonFgPaint);
            canvas.restore();
            canvas.save();
            canvas.translate(width - buttonSize / 2, height / 2);
            canvas.scale(buttonSize / 100f, buttonSize / 100f);
            buttonPaint.setColor(accepting ? 0xC000FF00 : 0xC0FFFF00);
            canvas.drawRect(-50f, -50f, 50f, 50f, buttonPaint);
            Path acceptTick = new Path();
            acceptTick.moveTo(-10f, 40f);
            acceptTick.lineTo(40f, -10f);
            acceptTick.lineTo(30f, -20f);
            acceptTick.lineTo(-10f, 20f);
            acceptTick.lineTo(-30f, 0f);
            acceptTick.lineTo(-40f, 10f);
            acceptTick.close();
            canvas.drawPath(acceptTick, buttonFgPaint);
            canvas.restore();
        } else if (coordinates != null) {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(width / 800f, width / 800f);
            BoardDrawable.drawPath(canvas, coordinates);
            canvas.restore();
        }
    }
}
