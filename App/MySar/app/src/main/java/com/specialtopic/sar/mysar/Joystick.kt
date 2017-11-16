package com.specialtopic.sar.mysar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams

enum class POS
{
    UP,
    UPRIGHT,
    RIGHT,
    DOWNRIGHT,
    DOWN,
    DOWNLEFT,
    LEFT,
    UPLEFT,
    NONE
}

class Joystick constructor(context : Context , layout : ViewGroup , stick_res_id : Int )
{
    private var stickAlpha = 200
    private var layoutAlpha = 200
    private var offset = 0

    private var mContext : Context = context
    private var mLayout : ViewGroup = layout
    private var params : LayoutParams = mLayout.layoutParams
    private var stick : Bitmap = BitmapFactory.decodeResource(mContext.resources, stick_res_id)
    private var stickWidth : Int = stick.width
    private var stickHeight : Int = stick.height

    private var positionX : Int = 0
    private var positionY : Int = 0
    private var minDistance : Int = 0
    private var distance : Float = 0F
    private var angle : Float = 0F

    private var drawCanvas : DrawCanvas = DrawCanvas(mContext)
    private var paint : Paint = Paint()

    private var touchState : Boolean = false

    fun drawStick(arg1 : MotionEvent)
    {
        positionX = (arg1.x - (params.width / 2)).toInt()
        positionY = (arg1.y - (params.height / 2)).toInt()
        distance = (Math.sqrt(Math.pow(positionX.toDouble(), 2.toDouble()) + Math.pow(positionY.toDouble(), 2.toDouble()))).toFloat()
        angle = (calcAngle(positionX.toDouble(), positionY.toDouble())).toFloat()


        if(arg1.action == MotionEvent.ACTION_DOWN)
        {
            if(distance <= (params.width / 2) - offset)
            {
                drawCanvas.position(arg1.x , arg1.y, stickWidth, stickHeight)
                draw()
                touchState = true
            }
        }
        else if(arg1.action == MotionEvent.ACTION_MOVE && touchState)
        {
            when
            {
                distance <= (params.width / 2) - offset -> {
                    drawCanvas.position(arg1.x , arg1.y, stickWidth, stickHeight)
                    draw()
                }
                distance > (params.width / 2) - offset -> {
                    var x : Float = (Math.cos(Math.toRadians(calcAngle(positionX.toDouble(), positionY.toDouble())))
                            * ((params.width / 2) - offset)).toFloat()
                    var y : Float = (Math.sin(Math.toRadians(calcAngle(positionX.toDouble(), positionY.toDouble())))
                            * ((params.height / 2) - offset)).toFloat()
                    x += (params.width / 2)
                    y += (params.height / 2)
                    drawCanvas.position(x, y, stickWidth, stickHeight)
                    draw()
                }
                else -> mLayout.removeView(drawCanvas)
            }
        }
        else if(arg1.action == MotionEvent.ACTION_UP)
        {
            mLayout.removeView(drawCanvas)
            touchState = false
        }
    }

    fun getPosition() : IntArray
    {
        return when (distance > minDistance && touchState)
        {
            true -> intArrayOf(positionX, positionY)
            false -> intArrayOf(0,0)
        }
    }

    fun getX() : Int
    {
        if(distance > minDistance && touchState)
        {
            return positionX
        }
        return 0
    }

    fun getY() : Int
    {
        if(distance > minDistance && touchState)
        {
            return positionY
        }
        return 0
    }

    fun getAngle() : Float
    {
        if(distance > minDistance && touchState)
        {
            return angle
        }
        return 0F
    }

    fun getDistance() : Float
    {
        if(distance > minDistance && touchState)
        {
            return distance
        }
        return 0F
    }

    fun setMinimumDistance(minDistance : Int)
    {
        this.minDistance = minDistance
    }

    fun getMinimumDistance() : Int
    {
        return minDistance
    }

    fun get8Direction() : POS
    {
        if(distance > minDistance && touchState)
        {
            when
            {
                angle >= 247.5 && angle < 292.5 -> return POS.UP
                angle >= 292.5 && angle < 337.5 -> return POS.UPRIGHT
                angle >= 337.5 || angle < 22.5 -> return POS.RIGHT
                angle >= 22.5 && angle < 67.5 -> return POS.DOWNRIGHT
                angle >= 67.5 && angle < 112.5 -> return POS.DOWN
                angle >= 112.5 && angle < 157.5 -> return POS.DOWNLEFT
                angle >= 157.5 && angle < 202.5 -> return POS.LEFT
                angle >= 202.5 && angle < 247.5 -> return POS.UPLEFT
            }
        }
        return POS.NONE
    }

    fun get4Direction() : POS
    {
        if(distance > minDistance && touchState)
        {
            when
            {
                angle >= 225 && angle < 315 -> return POS.UP
                angle >= 315 || angle < 45 -> return POS.RIGHT
                angle >= 45 && angle < 135 -> return POS.DOWN
                angle >= 135 && angle < 225 -> return POS.LEFT
            }
        }

        return POS.NONE
    }

    fun setOffset(offset : Int)
    {
        this.offset = offset
    }

    fun setStickAlpha(alpha : Int)
    {
        stickAlpha = alpha
        paint.alpha = stickAlpha
    }

    fun getStickAlpha() : Int
    {
        return stickAlpha
    }

    fun setLayoutAlpha(alpha : Int)
    {
        layoutAlpha = alpha
        mLayout.background.alpha = layoutAlpha
    }

    fun getLayoutAlpha() : Int
    {
        return layoutAlpha
    }

    fun setStickSize(width : Int, height : Int)
    {
        stick = Bitmap.createScaledBitmap(stick, width, height, false)
        stickWidth = stick.width
        stickHeight = stick.height
    }

    fun setStickWidth(width : Int)
    {
        stick = Bitmap.createScaledBitmap(stick, width, stickHeight, false)
        stickWidth = stick.width
    }

    fun setStickHeight(height : Int)
    {
        stick = Bitmap.createScaledBitmap(stick, stickWidth, height, false)
        stickHeight = stick.height
    }

    fun getStickWidth() : Int
    {
        return stickWidth
    }

    fun getStickHeight() : Int
    {
        return stickHeight
    }

    fun setLayoutSize(width : Int, height : Int)
    {
        params.width = width
        params.height = height
    }

    fun getLayoutWidth() : Int
    {
        return params.width
    }

    fun getLayoutHeight() : Int
    {
        return params.height
    }

    private fun calcAngle(x : Double, y : Double) : Double
    {
        when
        {
            x >= 0 && y >= 0 -> return Math.toDegrees(Math.atan(y / x ))
            x < 0 && y >= 0 -> return Math.toDegrees(Math.atan(y  / x)) + 180
            x < 0 && y < 0 -> return Math.toDegrees(Math.atan(y / x)) + 180
            x >= 0 && y < 0 -> return Math.toDegrees(Math.atan(y / x)) + 360
        }
        return 0.toDouble()
    }

    private fun draw()
    {
        try
        {
            mLayout.removeView(drawCanvas)
        }
        catch (e : Exception)
        { }

        mLayout.addView(drawCanvas)
    }

}

private class DrawCanvas(mContext : Context) : View(mContext)
{
    var xPos : Float = 0F
    var yPos : Float = 0F

    fun onDraw(canvas : Canvas, stick : Bitmap, paint : Paint)
    {
        canvas.drawBitmap(stick, xPos, yPos, paint)
    }

    fun position(pos_x : Float, pos_y : Float, stick_width : Int, stick_height : Int)
    {
        xPos = pos_x - (stick_width / 2)
        yPos = pos_y - (stick_height / 2)
    }
}