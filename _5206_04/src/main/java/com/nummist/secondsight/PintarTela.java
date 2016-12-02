package com.nummist.secondsight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by paulo on 06/11/16.
 */

public class PintarTela extends View {
    private Paint paint = new Paint();
    private Path path = new Path();

    //Esta classe também herdará da classe “View” e utilizará dois objetos, do tipo “Paint” e “Path”.
    public PintarTela(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(6);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        float eventX = motionEvent.getX();
        float eventY = motionEvent.getY();
        switch (motionEvent.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(eventX, eventY);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(eventX, eventY);
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }
};

