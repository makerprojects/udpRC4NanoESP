/*
 *  (C) Copyright 2017 Gregor Schlechtriem (http://www.pikoder.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *  	Gregor Schlechtriem
 */

package com.udprc4nanoesp;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekBar extends SeekBar {

	public VerticalSeekBar(Context context) {
		super(context);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	protected void onDraw(Canvas c) {
		c.rotate(-90);
		c.translate(-getHeight(), 0);

		super.onDraw(c);
	}

	private OnSeekBarChangeListener onChangeListener;
	@Override
	public void setOnSeekBarChangeListener(OnSeekBarChangeListener onChangeListener){
		this.onChangeListener = onChangeListener;
	}

	private int lastProgress = 0;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			onChangeListener.onStartTrackingTouch(this);
			setPressed(true);
			setSelected(true);
			break;
		case MotionEvent.ACTION_MOVE:
			super.onTouchEvent(event);
			int progress = getMax() - (int) (getMax() * event.getY() / getHeight());
			
			// Ensure progress stays within boundaries
			if(progress < 0) {progress = 0;}
			if(progress > getMax()) {progress = getMax();}
			setProgress(progress);	// Draw progress
			if(progress != lastProgress) {
				// Only enact listener if the progress has actually changed
				lastProgress = progress;
				onChangeListener.onProgressChanged(this, progress, true);
			}
			
			onSizeChanged(getWidth(), getHeight() , 0, 0);
			setPressed(true);
			setSelected(true);
			break;
		case MotionEvent.ACTION_UP:
			onChangeListener.onStopTrackingTouch(this);
			setPressed(false);
			setSelected(false);
			break;
		case MotionEvent.ACTION_CANCEL:
			super.onTouchEvent(event);
			setPressed(false);
			setSelected(false);
			break;
		}
		return true;
	}

	public synchronized void setProgressAndThumb(int progress) {
		setProgress(progress);
		onSizeChanged(getWidth(), getHeight() , 0, 0);
		if(progress != lastProgress) {
			// Only enact listener if the progress has actually changed
			lastProgress = progress;
			onChangeListener.onProgressChanged(this, progress, true);
		}
	}

	public synchronized void setMaximum(int maximum) {
		setMax(maximum);
	}

	public synchronized int getMaximum() {
		return getMax();
	}
}