/*
 * Copyright 2013 the original author or authors
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

package android.orm.playground;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.orm.DAO;
import android.orm.dao.ErrorHandler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

public abstract class Fragment extends DialogFragment {

    @Nullable
    private DAO.Async mDAO;
    @Nullable
    private final ErrorHandler.Factory<Activity> mFactory;

    protected Fragment() {
        this(null);
    }

    protected Fragment(@Nullable final ErrorHandler.Factory<Activity> factory) {
        super();

        mFactory = factory;
    }

    @NonNull
    protected abstract DAO.Async create(@NonNull final Context context);

    @Override
    public final void onAttach(@NonNull final Activity activity) {
        super.onAttach(activity);

        mDAO = create(activity.getBaseContext());
        if (mFactory != null) {
            mDAO.setErrorHandler(mFactory.create(activity));
        }
        afterAttach(activity);
    }

    @Override
    public final void onResume() {
        super.onResume();
        assert mDAO != null;
        mDAO.start();
        afterResume();
    }

    @Override
    public final void onPause() {
        beforePause();
        assert mDAO != null;
        mDAO.pause();
        super.onPause();
    }

    @Override
    public final void onDestroy() {
        beforeDestroy();
        assert mDAO != null;
        mDAO.stop();
        mDAO = null;
        super.onDestroy();
    }

    @NonNull
    protected final DAO.Async getDAO() {
        if (mDAO == null) {
            throw new UnsupportedOperationException("You are accessing DAO too early or to late");
        }
        return mDAO;
    }

    @NonNull
    protected static Form.Builder form(@NonNls @NonNull final String name) {
        return Form.builder(name);
    }

    protected void afterAttach(@NonNull final Activity activity) {
    }

    protected void afterResume() {
    }

    protected void beforePause() {
    }

    protected void beforeDestroy() {
    }
}
