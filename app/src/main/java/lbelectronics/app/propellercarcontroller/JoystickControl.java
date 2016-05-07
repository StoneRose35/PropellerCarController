package lbelectronics.app.propellercarcontroller;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philipp on 18.04.15.
 */
public class JoystickControl extends View implements BackToCenterListener{
    private Paint backgroundpaint,pointpaint;
    private int backgroundcolor,pointcolor;
    private int pointX,pointY,pointdiam;
    private float xval,yval;
    private float dx,dy;
    private Rect bgrect;
    private Point middlepoint;
    private boolean isMoving;
    private List<JoystickEventListener> listeners;

    public Point getCurrentValue()
    {
        Point res=new Point();
        res.set(pointX,pointY);
        return res;
    }

    public JoystickControl(Context context)
    {
        super(context);
        init();
    }

    public JoystickControl(Context context,AttributeSet attributeSet)
    {
        super(context,attributeSet);
        init();
    }

    private void init()
    {
        Resources resources=getResources();
        backgroundpaint=new Paint();
        backgroundpaint.setStyle(Paint.Style.FILL);
        backgroundpaint.setColor(resources.getColor(R.color.backgroundcolor));
        pointpaint=new Paint();
        pointpaint.setStyle(Paint.Style.FILL);
        pointpaint.setColor(resources.getColor(R.color.pointcolor));
        bgrect=new Rect(0,0,this.getMeasuredWidth(),getMeasuredHeight());
        middlepoint=new Point();
        middlepoint.set(this.pointX,this.pointY);
        pointdiam=80;
        listeners=new ArrayList<JoystickEventListener>(0);
        xval=0.5f;
        yval=0.5f;

    }

    @Override
    public void onMeasure(int width,int height)
    {
        int widthFinal,heightFinal;
        if(width<30)
        {
            widthFinal=30;
        }
        else
        {
            widthFinal=width;
        }
        if(height<30)
        {
            heightFinal=30;
        }
        else
        {
            heightFinal=height;
        }
        setMeasuredDimension(widthFinal,heightFinal);
        bgrect=new Rect(0,0,this.getMeasuredWidth(),getMeasuredHeight());
        pointX=(int)(this.getMeasuredWidth()*xval);
        pointY=(int)(this.getMeasuredHeight()*yval);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        canvas.drawRect(bgrect,backgroundpaint);
        canvas.drawCircle(this.pointX,this.pointY,pointdiam,pointpaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int action;
        action=event.getAction();
        if(event.getAction()==MotionEvent.ACTION_DOWN)
        {
            if(Math.sqrt((event.getX()-pointX)*(event.getX()-pointX) + (event.getY()-pointY)*(event.getY()-pointY))<pointdiam)
            {
                isMoving=true;
               xval=event.getX()/getMeasuredWidth();
               yval=event.getY()/getMeasuredHeight();
                invalidate();
                for (int k = 0; k < listeners.size(); k++) {
                    listeners.get(k).startDragging(ConverterFunctionX(xval), ConverterFunctionY(yval));

                }
                return true;
            }
        }
        else if(event.getAction()==MotionEvent.ACTION_MOVE)
        {
            if(isMoving)
            {
                xval= event.getX()/getMeasuredWidth();
                yval = event.getY()/getMeasuredHeight();
                invalidate();
                for (int k = 0; k < listeners.size(); k++) {
                    listeners.get(k).startDragging(ConverterFunctionX(xval), ConverterFunctionY(yval));

                }
                return true;
            }
        }
        else if(event.getAction()==MotionEvent.ACTION_UP)
        {
            isMoving=false;
            //GoBackAnimation goBackAnimation=new GoBackAnimation(this);
            //goBackAnimation.run();
            Thread anim=new Thread(new GoBackAnimation(this));
            anim.start();
        }
        return super.onTouchEvent(event);
    }

    public void registerEventListener(JoystickEventListener eventListener)
    {
        this.listeners.add(eventListener);
    }

    public void SetCalibDelta(float delta_X,float delta_Y)
    {
        dx=delta_X;
        dy=delta_Y;
    }

    public void IsAtCenter()
    {
        for (int k = 0; k < listeners.size(); k++) {
            listeners.get(k).startDragging(ConverterFunctionX(0.5), ConverterFunctionY(0.5));

        }
    }

    /*
    * konvertiert ein X-Wert welcher von 0 bis 1 geht in ein von Android-Empfänger verwertbaren Wert
    * */
    private int ConverterFunctionX(double xv)
    {
        double dval;
        dval=(xv-0.5)*2.0*255+dx;
        return (int)dval;
    }
    /*
    * konvertiert ein Y-Wert welcher von 0 bis 1 geht in ein von Android-Empfänger verwertbaren Wert
    * */
    private int ConverterFunctionY(double yv)
    {
        double dval;
        dval=(yv-0.5)*2.0*255+dy;
        return (int)dval;
    }


    class GoBackAnimation implements Runnable
    {
        float xvalstart,yvalstart;
        BackToCenterListener listener;
        public GoBackAnimation(BackToCenterListener backToCenterListener)
        {
            listener=backToCenterListener;
        }
        public void run()
        {
            double distance;
            int steps;
            xvalstart=xval;
            yvalstart=yval;
            distance=Math.sqrt((xvalstart-0.5)*(xvalstart-0.5) + (yvalstart-0.5)*(yvalstart-0.5));
            steps=(int)(distance*40);
            for(int k=0;k<steps;k++)
            {
                JoystickControl.this.xval=(float)(xvalstart + (0.5-xvalstart)*(float)k/(float)(steps-1));
                JoystickControl.this.yval=(float)(yvalstart + (0.5-yvalstart)*(float)k/(float)(steps-1));
                pointX=(int)(JoystickControl.this.getMeasuredWidth()*xval);
                pointY=(int)(JoystickControl.this.getMeasuredHeight()*yval);
                JoystickControl.this.postInvalidate();
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException exc)
                {
                    exc.printStackTrace();
                }
            }
            listener.IsAtCenter();

        }
    }
}

interface BackToCenterListener
{
    void IsAtCenter();
}