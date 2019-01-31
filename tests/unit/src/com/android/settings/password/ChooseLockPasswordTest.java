/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.password;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.not;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ChooseLockPasswordTest {
    private Instrumentation mInstrumentation;
    private Context mContext;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
    }

    @Test
    public void clearNotVisible_when_activityLaunchedInitially() {
        mInstrumentation.startActivitySync(new Intent(mContext, ChooseLockPassword.class));
        onView(withId(R.id.clear_button)).check(matches(
                withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void clearNotEnabled_when_nothingEntered() {
        mInstrumentation.startActivitySync(new Intent(mContext, ChooseLockPassword.class));
        onView(withId(R.id.password_entry)).perform(ViewActions.typeText("1234"))
                .perform(pressKey(KeyEvent.KEYCODE_ENTER));
        onView(withId(R.id.clear_button)).check(matches(isDisplayed()))
            .check(matches(not(isEnabled())));
    }

    @Test
    public void clearEnabled_when_somethingEnteredToConfirm() {
        mInstrumentation.startActivitySync(new Intent(mContext, ChooseLockPassword.class));
        onView(withId(R.id.password_entry)).perform(ViewActions.typeText("1234"))
                .perform(pressKey(KeyEvent.KEYCODE_ENTER))
                .perform(ViewActions.typeText("1"));
        // clear should be present if text field contains content
        onView(withId(R.id.clear_button)).check(matches(isDisplayed()));
    }
}