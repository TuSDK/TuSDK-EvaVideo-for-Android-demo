/** 
 * TuSDKCore
 * TuMaskRegionView.java
 *
 * @author 		Clear
 * @Date 		2014-12-1 下午3:04:49 
 * @Copyright 	(c) 2014 tusdk.com. All rights reserved.
 * 
 */
package org.lsque.tusdkevademo.playview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.core.view.ViewCompat;

import org.lasque.tusdkpulse.core.struct.TuSdkSize;
import org.lasque.tusdkpulse.core.struct.ViewSize;
import org.lasque.tusdkpulse.core.utils.ContextUtils;
import org.lasque.tusdkpulse.core.utils.RectHelper;
import org.lasque.tusdkpulse.core.utils.anim.AccelerateDecelerateInterpolator;

/**
 * 裁剪区域视图
 * 
 * @author Clear
 */
public class TuMaskRegionView extends View
{
	/** 是否已经布局成功 */
	protected boolean isLayouted;

	public TuMaskRegionView(Context context)
	{
		super(context);
		this.initView();
	}

	public TuMaskRegionView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.initView();
	}

	public TuMaskRegionView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		this.initView();
	}

	/** 初始化视图 */
	protected void initView()
	{
		// 关闭该视图硬件加速
		this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		this.initFirstLayout();
	}

	/** 初始化第一次加载视图布局事件，获取视图宽高 */
	private void initFirstLayout()
	{
		ViewTreeObserver vto = this.getViewTreeObserver();
		vto.addOnPreDrawListener(preDrawListener);
	}

	/** 预绘图事件，获取视图高宽 */
	private OnPreDrawListener preDrawListener = new OnPreDrawListener()
	{
		@Override
		public boolean onPreDraw()
		{
			getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
			if (!isLayouted)
			{
				isLayouted = true;
				onLayouted();
			}
			return false;
		}
	};

	/** 视图已布局 */
	protected void onLayouted()
	{
		if (mNeedDraw)
		{
			this.setRegionRect(this.computerRect(this.getRegionRatio()));
			mNeedDraw = false;
		}
	}

	/** 区域长宽 */
	private TuSdkSize mRegionSize;
	/** 视频预览显示比例 (默认：0， 0 <= RegionRatio, 当设置为0时全屏显示) */
	private float mRegionRatio = 0;
	/** 选区信息 */
	private Rect mRegionRect;
	/** 边缘覆盖区域颜色 (默认:Color.TRANSPARENT) */
	private int mEdgeMaskColor = Color.TRANSPARENT;
	/** 边缘线颜色 (默认:Color.TRANSPARENT) */
	private int mEdgeSideColor = Color.TRANSPARENT;
	/** 边缘线宽度 (默认:0) */
	private int mEdgeSideWidth;
	/** 是否需要绘图 */
	private boolean mNeedDraw;

	/** 区域长宽 */
	public TuSdkSize getRegionSize()
	{
		return mRegionSize;
	}

	/** 选区信息 */
	public void setRegionSize(TuSdkSize mRegionSize)
	{
		this.mRegionSize = mRegionSize;

		if (mRegionSize != null)
		{
			this.mRegionRatio = mRegionSize.width / (float) mRegionSize.height;
			this.autoShowForRegionRatio();
			this.setRegionRect(this.computerRect(mRegionRatio));
		}
	}

	/** 视频预览显示比例 (默认：0， 0 <= RegionRatio, 当设置为0时全屏显示) */
	public float getRegionRatio()
	{
		return mRegionRatio;
	}

	/** 视频预览显示比例 (默认：0， 0 <= mRegionRatio, 当设置为0时全屏显示) */
	public Rect setRegionRatio(float mRegionRatio)
	{
		this.mRegionRatio = mRegionRatio;
		Rect rect = this.computerRect(this.mRegionRatio);
		if (isLayouted)
		{
			this.setRegionRect(rect);
		}
		else
		{
			mNeedDraw = true;
		}
		this.autoShowForRegionRatio();
		return rect;
	}

	/** 根据比例信息决定是否显示 */
	public void autoShowForRegionRatio()
	{
		if (mRegionRatio <= 0)
		{
			ViewCompat.setAlpha(this, 0);
		} 
		else if (ViewCompat.getAlpha(this) == 0)
		{	 
			ViewCompat.setAlpha(this, 1);
		}
	}

	/** 解决某些机型(华为)软件home栏目改变视图大小，造成视图无法响应 */
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if (changed)
		{
			this.setRegionRatio(this.getRegionRatio());
		}
	}

	/** 设置选取范围 */
	private void setRegionRect(Rect regionRect)
	{
		this.mRegionRect = regionRect;
		this.invalidate();
	}

	/** 计算区域 */
	private Rect computerRect(float mRegionRatio)
	{
		return RectHelper.computerCenter(ViewSize.create(this), mRegionRatio);
	}

	/** 选区信息 */
	public Rect getRegionRect()
	{
		return mRegionRect;
	}

	/** 边缘覆盖区域颜色 (默认:Color.TRANSPARENT) */
	public int getEdgeMaskColor()
	{
		return mEdgeMaskColor;
	}

	/** 边缘覆盖区域颜色 (默认:Color.TRANSPARENT) */
	public void setEdgeMaskColor(int mEdgeMaskColor)
	{
		this.mEdgeMaskColor = mEdgeMaskColor;
	}

	/** 边缘线颜色 (默认:Color.TRANSPARENT) */
	public int getEdgeSideColor()
	{
		return mEdgeSideColor;
	}

	/** 边缘线颜色 (默认:Color.TRANSPARENT) */
	public void setEdgeSideColor(int mEdgeSideColor)
	{
		this.mEdgeSideColor = mEdgeSideColor;
	}

	/** 边缘线宽度 (默认:0) */
	public int getEdgeSideWidth()
	{
		return mEdgeSideWidth;
	}

	/** 边缘线宽度 (默认:0) */
	public void setEdgeSideWidth(int mEdgeSideWidth)
	{
		this.mEdgeSideWidth = mEdgeSideWidth;
	}
	
	/** 边缘线宽度 (单位:DP) */
	public void setEdgeSideWidthDP(int mEdgeSideWidthDP)
	{
		this.mEdgeSideWidth = ContextUtils.dip2px(this.getContext(), mEdgeSideWidthDP);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		this.drawRegion(canvas, this.getRegionRect());
		super.onDraw(canvas);
	}

	/** 绘图范围 */
	private RectF mRect = new RectF();
	/** 圆角路径 */
	private Path mPath = new Path();
	/** 绘图笔 */
	private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	{
		mPaint.setAntiAlias(true);
	}

	/** 绘制区域 */
	private void drawRegion(Canvas canvas, Rect rect)
	{
		// TLog.d("drawRegion: %s", rect);
		if (rect == null || canvas == null) return;

		mRect.set(0, 0, this.getMeasuredWidth(), this.getMeasuredHeight());
		
		float offset = 0;
		
		// 绘制边缘框
		if (this.getEdgeSideWidth() > 0)
		{
			mPaint.setColor(this.getEdgeSideColor());
			mPaint.setStrokeWidth(this.getEdgeSideWidth());
			mPaint.setStyle(Paint.Style.STROKE);
			// 边框默认居中，需要在内部绘制
			offset = this.getEdgeSideWidth() * 0.5f;
			canvas.drawRect(new RectF(rect.left + offset, rect.top + offset, rect.right - offset, rect.bottom - offset), mPaint);
		}

		mPath.reset();
		// 绘制镂空区域
		mPath.addRect(rect.left, rect.top, rect.right, rect.bottom, Direction.CW);

		mPath.close();

		canvas.clipPath(mPath, Op.DIFFERENCE);

		// 绘制背景
		mPaint.setColor(this.getEdgeMaskColor());
		mPaint.setStyle(Paint.Style.FILL);
		canvas.drawRect(mRect, mPaint);
	}

	/**************************** Region change ************************************/

	/** 改变范围比例 (使用动画) */
	public Rect changeRegionRatio(float mRegionRatio)
	{
		Rect rect = this.computerRect(mRegionRatio);

		if (this.mRegionRatio == mRegionRatio) return rect;
		
		if (mRegionRatio <= 0)
		{
			ViewCompat.animate(this).alpha(0).setDuration(260).setInterpolator(new AccelerateDecelerateInterpolator());
		}
		else if (ViewCompat.getAlpha(this) == 0)
		{
			ViewCompat.animate(this).alpha(1).setDuration(260).setInterpolator(new AccelerateDecelerateInterpolator());
		}
		
		this.getRegionChangeAnimation().startTo(rect);
		this.startAnimation(this.getRegionChangeAnimation());

		this.mRegionRatio = mRegionRatio;
		return rect;
	}

	/** 范围改变动画 */
	private RegionChangeAnimation mRegionChangeAnimation;

	/** 获取范围改变动画 */
	private RegionChangeAnimation getRegionChangeAnimation()
	{
		if (mRegionChangeAnimation == null)
		{
			mRegionChangeAnimation = new RegionChangeAnimation(this.getRegionRatio());
			mRegionChangeAnimation.setDuration(260);
			mRegionChangeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		}
		mRegionChangeAnimation.cancel();
		mRegionChangeAnimation.reset();
		return mRegionChangeAnimation;
	}

	/** 范围改变动画 */
	private class RegionChangeAnimation extends Animation
	{
		/** 当前比例 */
		private Rect mCurrent;

		/** 目标比例 */
		private Rect mTo;

		/** 差值 */
		private Rect mReduce;

		/** 范围改变动画 */
		public RegionChangeAnimation(float current)
		{
			mCurrent = computerRect(current);
		}

		/** 开始执行 */
		public void startTo(Rect to)
		{
			mTo = to;
			mReduce = new Rect(mTo.left - mCurrent.left, mTo.top - mCurrent.top, mTo.right - mCurrent.right, mTo.bottom - mCurrent.bottom);
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			interpolatedTime = 1 - interpolatedTime;

			mCurrent.left = mTo.left - (int) (mReduce.left * interpolatedTime);
			mCurrent.top = mTo.top - (int) (mReduce.top * interpolatedTime);
			mCurrent.right = mTo.right - (int) (mReduce.right * interpolatedTime);
			mCurrent.bottom = mTo.bottom - (int) (mReduce.bottom * interpolatedTime);

			setRegionRect(mCurrent);
		}
	}
}