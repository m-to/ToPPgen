/**
 * Copyright 2011 Laurence Gellert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.laurencegellert.androidwidgets;

import java.math.BigDecimal;

import android.content.Context;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

/**
 * Float Picker Widget is an open source Android based widget for picking a number using plus and
 * minus buttons, no typing required. Widget supports whole numbers, decimals, positive values, 
 * and negative values. Configuration options include increment size, number of decimal places, 
 * minimum value, maximum value, etc.  When user holds down button, the widget will continue
 * incrementing and accelerate.   Could be used for picking specific scientific values 
 * (pH, temperature), AM/FM radio stations, quantity of items to purchase in lots.
 * 
 * Configuration options, specified in the XML layout as attributes:
 * <ul>
 * <li>button_width - int - width of add and subtract buttons, defaults to 40.</li>
 * <li>button_height - int - height of add and subtract buttons, defaults to LayoutParams.FILL_PARENT.</li>
 * <li>button_text_size - int - size of text labels for buttons, defaults to 20.</li>
 * <li>edittext_width - int - text field width field, defaults to 80.</li>
 * <li>edittext_height - int - text field height, defaults to LayoutParams.FILL_PARENT.</li>
 * <li>edittext_size - int - size of text to use, integer, defaults to 20.</li>
 * <li>add_button_label - String - content of add button, defaults to '+'.</li>
 * <li>subtract_button_label - String - content of subtract button, defaults to '-'.</li>
 * <li>minimum_value - float - minimum value allowed, defaults to 0f.</li>
 * <li>maximum_value - float - maximum value allowed, defaults to 100f.</li>
 * <li>default_value - float - default value, defaults to <i>minimum_value</i>.</li>
 * <li>increment_amount - float - amount to add/subtract when button pressed, defaults to 1f.</li>
 * <li>decimal_places - int - number of decimals to display, defaults to 0.</li>
 * <li>starting_repeat_rate - int - initial milliseconds between repeats, defaults to 200, must be above repeat_acceleration.</li>
 * <li>repeat_acceleration - int - milliseconds to decrement the repeat interval on
 * each repeat, defaults to 50, do not set below 25.</li>
 * </ul>
 * 
 * @author Laurence Gellert
 * <a href="http://www.laurencegellert.com/software/android-float-picker-widget/">Float Picker Widget Home Page</a>
 * 
 */
public class FloatPickerWidget extends RelativeLayout {

	private static final boolean	DEBUG_MODE = false; //set to false in production
	private static final String 	LOG_IDENTIFIER = "FloatPickerWidget";
	
	private static final int 		SUBTRACT_BUTTON_ID = 1;
	private static final int 		ADD_BUTTON_ID = 2;
	private static final int 		TEXT_ID = 3;
	
	private static final String 	DEFAULT_ADD_BUTTON_LABEL = "+";
	private static final String 	DEFAULT_SUBTRACT_BUTTON_LABEL = "-";
	
	private static final int 		DEFAULT_BUTTON_WIDTH = 40;
	private static final int 		DEFAULT_BUTTON_TEXT_SIZE = 20;
	
	private static final int 		DEFAULT_TEXTAREA_WIDTH = 80;
	private static final int 		DEFAULT_TEXTAREA_TEXT_SIZE = 20;
	
	private static final float 		DEFAULT_MINIMUM_VALUE = 0f;
	private static final float 		DEFAULT_MAXIMUM_VALUE = 100f;
	private static final float 		DEFAULT_INCREMENT = 1f;
	private static final int 		DEFAULT_DECIMAL_PLACES = 0;
	
	//how often in (ms) the click will be repeated when button held down to start with
	private static final int 		DEFAULT_REPEAT_RATE = 200;
	private static final int		DEFAULT_REPEAT_ACCELERATION = 50;
	
	//the fastest click interval when the button is held down
	private static final int 		MINIMUM_REPEATE_RATE = 25;
	
	private static final int		REPEAT_SUBTRACT = -1;
	private static final int		REPEAT_ADD = 1;
	private static final int		REPEAT_STOPPED = 0;
	
	private float value;	// the main variable, the whole point of this object

	private float incrementStep;
	private int decimalPlaces;
	private float max;
	private float min;
	
	private EditText editText;
	private Button buttonAdd;
	private Button buttonSubtract;
		
	private String addButtonLabel;
	private String subtractButtonLabel;
	
	private final int buttonWidth;
	private final int buttonHeight;
	private final int buttonTextSize;
	
	private final int editTextWidth;
	private final int editTextHeight;
	private final int editTextSize;
	
	private int startingRepeatRate;
	
	// gets decreased as the button is held down (which causes event to fire faster)
	private int currentRepeatRate = DEFAULT_REPEAT_RATE; 
	private Handler repeatHandler = new Handler();
	private int repeatAcceleration;
	private ChangeListener changeListener;
	private ButtonRepeater buttonRepeater = new ButtonRepeater();

	private int repeatState = 0;  //corresponds to REPEAT_SUBTRACT, REPEAT_ADD, or REPEAT_STOPPED
	

	/**
	 * Default constructor called from Android.
	 * Populates the local variables based on XML attributes configured in layout.
	 * Assigns default values if not specified.
	 * 
	 * @param context  Android Context
	 * @param attributeSet XML attributes
	 */
	public FloatPickerWidget(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		
		// NOTE: for the following section, wanted to use getAttributeFloatValue(String namespace, String attribute, float defaultValue)
		// but it doesn't work:  http://code.google.com/p/android/issues/detail?id=16085
		// was throwing a 'not a float!' runtime exception no matter what!
		// work around is to use parseFloat with a try/catch which is pretty ugly ->  :[
		try {
			min = Float.parseFloat(attributeSet.getAttributeValue(null, "minimum_value"));
		} catch (Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute minimum_value, using default value. Exception: " + e);
			min = DEFAULT_MINIMUM_VALUE;
		}

		try {
			max = Float.parseFloat(attributeSet.getAttributeValue(null, "maximum_value"));
		} catch (Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute maximum_value, using default value. Exception: " + e);
			max = DEFAULT_MAXIMUM_VALUE;
		}

		try {
			value = Float.parseFloat(attributeSet.getAttributeValue(null, "default_value"));
		} catch (Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute default_value, using default value. Exception: " + e);
			value = DEFAULT_MINIMUM_VALUE;
		}

		try {
			incrementStep = Float.parseFloat(attributeSet.getAttributeValue(null, "increment_amount"));
		} catch (Exception e) {
			Log.w(LOG_IDENTIFIER, "Unable to parse float value for attribute increment_amount, using default value. Exception: " + e);
			incrementStep = DEFAULT_INCREMENT;
		}

		decimalPlaces = attributeSet.getAttributeIntValue(null, "decimal_places", DEFAULT_DECIMAL_PLACES);
		startingRepeatRate = attributeSet.getAttributeIntValue(null, "starting_repeat_rate", DEFAULT_REPEAT_RATE);
		repeatAcceleration = attributeSet.getAttributeIntValue(null, "repeat_acceleration", DEFAULT_REPEAT_ACCELERATION);

		addButtonLabel = attributeSet.getAttributeValue(null, "add_button_label");
		if(addButtonLabel == null) { addButtonLabel = DEFAULT_ADD_BUTTON_LABEL; }
		subtractButtonLabel = attributeSet.getAttributeValue(null, "subtract_button_label");
		if(subtractButtonLabel == null) { subtractButtonLabel = DEFAULT_SUBTRACT_BUTTON_LABEL; }	
		
		buttonWidth = attributeSet.getAttributeIntValue(null, "button_width", DEFAULT_BUTTON_WIDTH);
		buttonHeight = attributeSet.getAttributeIntValue(null, "button_height", LayoutParams.FILL_PARENT);
		buttonTextSize = attributeSet.getAttributeIntValue(null, "button_text_size", DEFAULT_BUTTON_TEXT_SIZE);

		editTextWidth = attributeSet.getAttributeIntValue(null, "edittext_width", DEFAULT_TEXTAREA_WIDTH);
		editTextHeight = attributeSet.getAttributeIntValue(null, "edittext_height", LayoutParams.FILL_PARENT);
		editTextSize = attributeSet.getAttributeIntValue(null, "edittext_size", DEFAULT_TEXTAREA_TEXT_SIZE);
		
		create(context, attributeSet);

		return;
	}
	
	/**
	 * Create all the sub widgets and arrange them appropriately.
	 * 
	 * @param context  Android Context
	 * @param attributeSet XML attributes
	 */
	private void create(Context context, AttributeSet attributeSet) {

		TriggerAction addAction = new AddButtonTrigger();
		TriggerAction subtractAction = new SubtractButtonTrigger();
		buttonAdd = buildButton(context, attributeSet, addButtonLabel, ADD_BUTTON_ID, REPEAT_ADD, addAction);
		buttonSubtract = buildButton(context, attributeSet, subtractButtonLabel, SUBTRACT_BUTTON_ID, REPEAT_SUBTRACT, subtractAction);
		
		editText = buildEditText(context, attributeSet);

		RelativeLayout.LayoutParams lp;

		lp = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		addView(editText, lp);

		lp = new RelativeLayout.LayoutParams(buttonWidth, buttonHeight);
		lp.addRule(RelativeLayout.RIGHT_OF, TEXT_ID);
		addView(buttonSubtract, lp);

		lp = new RelativeLayout.LayoutParams(buttonWidth, buttonHeight);
		lp.addRule(RelativeLayout.RIGHT_OF, SUBTRACT_BUTTON_ID);
		addView(buttonAdd, lp);

	}

	
	/**
	 * Build a button, and wire the events.
	 * 
	 * Defaults to centered gravity (text is centered inside button).
	 * 
	 * Button text size, button text (label) are configurable through XML.
	 * 
	 * @param context
	 * @param attributeSet Attributes passed from XML configuration.
	 * @param label Label for the button.
	 * @param buttonId Android layout ID of the button.
	 * @param repeatDir Direction to repeat in - REPEAT_ADD or REPEAT_SUBTRACT.
	 * @parm action What TriggerAction to call when button is clicked.
	 * @return Button
	 */
	private Button buildButton(Context context, AttributeSet attributeSet, String label, int buttonId, final int repeatDir, final TriggerAction action) {
		
		Button button = new Button(context, attributeSet);
		button.setTextSize(buttonTextSize);
		button.setText(label);
		button.setGravity(Gravity.CENTER);
		button.setId(buttonId);

		// on a long click, start repeating
		button.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View arg0) {
				startRepeating(repeatDir);
				return false;
			}
		});

		// when the button is touched
		button.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				
				// immediately trigger the action when they tap the button
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					action.click();
				}
				
				// on release, stop repeating
				if (event.getAction() == MotionEvent.ACTION_UP) {
					startRepeating(0);
				}
				return false;
			}
		});

		return button;
	}

	/**
	 * Build the text field.
	 * Defaults to centered gravity (text is centered inside widget).
	 * Text size is configurable from XML.
	 *   
	 * Displays a rounded version of the internal 'value', based on
	 * the configured number of decimal places.
	 * 
	 * @param context
	 * @param attributeSet Attributes passed from XML configuration.
	 * @return Text field for value display
	 */
	private EditText buildEditText(Context context, AttributeSet attributeSet) {
		
		if(DEBUG_MODE) {
			Log.d(LOG_IDENTIFIER, "Setting editTextSize to = " + editTextSize);
		}
		
		EditText editText = new EditText(context, attributeSet);
		editText.setTextSize(editTextSize);
		editText.setId(TEXT_ID);
		editText.setGravity(Gravity.CENTER);
		editText.setText(getRoundedValue());
		editText.setFocusable(false); 
		editText.setClickable(false);
		editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		return editText;
	}
	
	/**
	 * Add to the value by the increment step.
	 */
	protected void add() {
		float previous = value;
		value = Math.min(max, value + incrementStep);

		if (value != previous) {
			updateAndAlertListener();
		}

		return;
	}

	/**
	 * Subtract from the value by increment step.
	 */
	protected void subtract() {
		float previous = value;
		value = Math.max(min, value - incrementStep);

		if (value != previous) {
			updateAndAlertListener();
		}

		return;
	}
	
	/**
	 * Called after the value is changed by the add and subtract methods.
	 * Updates the text field with the human friendly version and fires event listener.
	 */
	private void updateAndAlertListener() {
		
		editText.setText(getRoundedValue());
		
		if (changeListener != null) {
			changeListener.onValueChange(this, value);
		}
	}
	
	/**
	 * Gets human friendly rounded version of 'value'.
	 * 
	 * @return String Rounded version of 'value' as a String.
	 */
	public String getRoundedValue() {
		BigDecimal bigDecimal = new BigDecimal(value).setScale(decimalPlaces, BigDecimal.ROUND_HALF_EVEN);
		return bigDecimal.toString();
	}

	/**
	 * Get the current value. Note this will be the actual float
	 * value which may have slight rounding differences compared 
	 * to what is on screen.
	 * 
	 * Use getRoundedValue() to get the pretty value.
	 * 
	 * @return value.
	 */
	public float getValue() {
		return value;
	}

	/**
	 * Set the value. Filters based on minimum and maximum options.
	 * 
	 * @param newValue  New float value to use.
	 * @return Resulting value.
	 */
	public float setValue(float newValue) {
		if (newValue > max) {
			newValue = max;
		}
		if (newValue < min) {
			newValue = min;
		}
		value = newValue;
		editText.setText(String.valueOf(value));
		editText.invalidate();

		if (changeListener != null) {
			changeListener.onValueChange(this, value);
		}

		return value;
	}

	/**
	 * Interface for objects to be notified of changes to the value.
	 * 
	 */
	public interface ChangeListener {
		public void onValueChange(FloatPickerWidget fpw, float value);
	}

	/**
	 * Set the listener to be notified of changes to the value.
	 * 
	 * @param listener
	 *            listener to be called.
	 */
	public void onValueChange(ChangeListener listener) {
		changeListener = listener;
		return;
	}

	/**
	 * Start auto repeating an increment/or decrement. Will stop when this is
	 * called with a mode of 0.
	 * 
	 * @param mode Corresponds to REPEAT_ADD, REPEAT_SUBTRACT, or REPEAT_STOPPED
	 */
	private void startRepeating(int mode) {
		repeatState = mode;
		if (repeatState != 0) {
			repeatHandler.postDelayed(buttonRepeater, currentRepeatRate);
		}
		return;
	}

	/**
	 * When button is held down, continue to trigger the action.
	 * 
	 * TODO: Could be refactored so the private inner class goes away:
	 *   According to (http://developer.android.com/guide/practices/design/performance.html),
	 *   sub section "Consider Package Instead of Private Access with Private Inner Classes",
	 *   there is a small performance hit to this approach.
	 * 
	 */
	private class ButtonRepeater implements Runnable {
		public void run() {
			trigger();
			
			if(DEBUG_MODE) {
				Log.d(LOG_IDENTIFIER, "currentRepeatRate = " + currentRepeatRate);
			}
			
			if (repeatState != REPEAT_STOPPED) {
				repeatHandler.postDelayed(buttonRepeater, currentRepeatRate);
				currentRepeatRate -= repeatAcceleration;
				if (currentRepeatRate < MINIMUM_REPEATE_RATE) {
					currentRepeatRate = MINIMUM_REPEATE_RATE;
					//when the floor is hit, go 2x
					trigger();
				}
			} else { // Restore to default repeat rate
				currentRepeatRate = startingRepeatRate;
			}
			return;
		}
		
		/**
		 * Changes the value in the direction based on repeatState.
		 */
		private void trigger() {
			if (repeatState == REPEAT_ADD) {
				add();
			}
			if (repeatState == REPEAT_SUBTRACT) {
				subtract();
			}
		}
	}
	
	/**
	 * Interface provides a pass through to button click events. 
	 * Used during button setup.
	 */
	public interface TriggerAction {
		public void click();
	}
	
	/**
	 * Triggers the add() method.
	 */
	private class AddButtonTrigger implements TriggerAction {
		public void click() {
			add();
		}
	}
	
	/**
	 * Triggers the subtract() method.
	 */
	private class SubtractButtonTrigger implements TriggerAction {
		public void click() {
			subtract();
		}
	}
	

}
