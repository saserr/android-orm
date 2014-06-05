/*
 * Copyright 2014 the original author or authors
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

package android.orm.tasks.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

public class ListItem extends CheckedTextView {

    public ListItem(final Context context) {
        super(context);
    }

    public ListItem(final Context context, final AttributeSet attributes) {
        super(context, attributes);
    }

    public ListItem(final Context context, final AttributeSet attributes, final int defStyle) {
        super(context, attributes, defStyle);
    }

    @Override
    protected final void onDraw(@NonNull final Canvas canvas) {
        if (isChecked()) {
            setTextColor(Color.GRAY);
            setPaintFlags(getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            setTextColor(Color.BLACK);
            setPaintFlags(getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        super.onDraw(canvas);
    }
}
