/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.startop.test;

import android.os.Bundle;

/**
 * This activity inflates a reasonably complex layout to see the impact of
 * layout inflation. The layout is supported by the viewcompiler, so this can be
 * used for testing precompiled layout performance.
 */
public class ComplexLayoutInflationActivity extends LayoutInflationActivity {
    protected void onCreate(Bundle savedInstanceState) {
        Bundle newState = savedInstanceState == null
                ? new Bundle() : new Bundle(savedInstanceState);
        newState.putInt(LAYOUT_ID, R.layout.activity_main);

        super.onCreate(newState);
    }
}
