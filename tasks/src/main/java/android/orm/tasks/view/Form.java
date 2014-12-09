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

import android.app.Activity;
import android.orm.model.Bindings;
import android.orm.model.Validator;
import android.orm.tasks.R;
import android.orm.tasks.model.Task;
import android.orm.util.Maybe;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import static android.orm.util.Validations.IsNotEmpty;

public class Form extends Fragment {

    private Controller mController = DUMMY_CONTROLLER;

    @Nullable
    private Task mEdited;

    private EditText mTitle;
    private Validator.Instance mValidator;

    private final View.OnKeyListener mSave = new View.OnKeyListener() {
        @Override
        public boolean onKey(final View view, final int code, @NotNull final KeyEvent event) {
            final boolean result;

            if (((code == KeyEvent.KEYCODE_ENTER)
                    || (code == KeyEvent.KEYCODE_DPAD_CENTER))
                    && (event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (mValidator.isValid()) {
                    final Task task;

                    final String title = mTitle.getText().toString();
                    if (mEdited == null) {
                        task = new Task(title);
                    } else {
                        mEdited.setTitle(title);
                        task = mEdited;
                    }

                    mController.save(task);
                }
                result = true;
            } else {
                result = false;
            }

            return result;
        }
    };

    public static Form create() {
        return new Form();
    }

    public Form() {
        super();
    }

    @Override
    public final View onCreateView(final LayoutInflater inflater,
                                   final ViewGroup container,
                                   final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.form, container, false);

        mTitle = (EditText) view.findViewById(R.id.edit_task_title);
        mTitle.setOnKeyListener(mSave);

        mValidator = Validator.instance()
                .with(IsNotEmpty, Bindings.text(mTitle), new Validator.Callback<Maybe<String>>() {

                    @Override
                    public void onValid(@Nullable final Maybe<String> title) {
                        mTitle.setError(null);
                    }

                    @Override
                    public void onInvalid(@Nullable final Maybe<String> title) {
                        mTitle.setError(getResources().getString(R.string.error_task_title_required));
                        mTitle.requestFocus();
                    }
                })
                .build();

        clear();

        return view;
    }

    public final void edit(final Task task) {
        mEdited = task;

        final String title = task.getTitle();
        mTitle.setText(title);
        mTitle.setSelection(title.length());
        mTitle.setHint(R.string.edit_task);
        mTitle.requestFocus();
    }

    public final void clear() {
        mEdited = null;

        mTitle.setText("");
        mTitle.setHint(R.string.new_task);
        mTitle.clearFocus();
    }

    public final void disable() {
        mTitle.setEnabled(false);
    }

    public final void enable() {
        mTitle.setEnabled(true);
    }

    @Override
    public final void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Controller)) {
            throw new IllegalStateException("Activity must implement TaskForm's controller.");
        }
        mController = (Controller) activity;
    }

    @Override
    public final void onDetach() {
        mController = DUMMY_CONTROLLER;
        super.onDetach();
    }

    public interface Controller {
        void save(final Task task);
    }

    private static final Controller DUMMY_CONTROLLER = new Controller() {
        @Override
        public void save(final Task task) {/* do nothing */}
    };
}
