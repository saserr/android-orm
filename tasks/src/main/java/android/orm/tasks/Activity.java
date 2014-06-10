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

package android.orm.tasks;

import android.content.res.Resources;
import android.orm.DAO;
import android.orm.access.Result;
import android.orm.tasks.data.Provider;
import android.orm.tasks.model.Task;
import android.orm.tasks.view.Form;
import android.orm.tasks.view.List;
import android.orm.util.Maybe;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Collection;

import static android.orm.sql.statement.Select.where;
import static android.orm.tasks.data.Provider.Routes.TaskById;
import static android.orm.tasks.data.Provider.Routes.Tasks;
import static android.widget.Toast.LENGTH_SHORT;
import static java.util.Collections.emptyList;

public class Activity extends ActionBarActivity implements Form.Controller, List.Controller {

    private static final Collection<Task> NO_TASKS = emptyList();
    private static final String EDITED_TASK_STATE = "edited_task"; //NON-NLS

    private DAO mDAO;
    private DAO.Access.Many mTasks;
    private long mEditedTask = Task.NO_ID;

    private Form mForm;
    private List mList;

    private final Runnable mClearForm = new Runnable() {
        @Override
        public void run() {
            mEditedTask = Task.NO_ID;
            mForm.clear();
            mForm.enable();
        }
    };

    private final Result.Observer<Integer> mShowDeleted = new Result.Observer<Integer>() {

        @Override
        public void onSomething(@Nullable final Integer deleted) {
            if ((deleted == null) || (deleted == 0)) {
                onNothing();
            } else {
                final Resources resources = getResources();
                final String message = resources.getQuantityString(R.plurals.info_tasks_deleted, deleted, deleted);
                Toast.makeText(Activity.this, message, LENGTH_SHORT).show();
            }
        }

        @Override
        public void onNothing() {
            Toast.makeText(Activity.this, R.string.info_no_tasks_deleted, LENGTH_SHORT).show();
        }
    };

    private final Runnable mSaveError = new Runnable() {
        @Override
        public void run() {
            mForm.enable();
            Toast.makeText(Activity.this, R.string.error_save_task, LENGTH_SHORT).show();
        }
    };

    private final Result.Callback<Collection<Task>> mShowTasks =
            new Result.Callback<Collection<Task>>() {
                @Override
                public void onResult(@NonNull final Maybe<Collection<Task>> result) {
                    final Collection<Task> tasks = result.getOrElse(null);
                    mList.show((tasks == null) ? NO_TASKS : tasks);
                }
            };

    public Activity() {
        super();
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDAO = DAO.local(this, Provider.DATABASE);
        mTasks = mDAO.at(Tasks);

        setContentView(R.layout.activity);
        final FragmentManager fragments = getSupportFragmentManager();
        if (savedInstanceState == null) {
            mForm = Form.create();
            mList = List.create();
            fragments.beginTransaction()
                    .add(R.id.task_form, mForm)
                    .add(R.id.tasks_list, mList)
                    .commit();
        } else {
            if (savedInstanceState.containsKey(EDITED_TASK_STATE)) {
                mEditedTask = savedInstanceState.getLong(EDITED_TASK_STATE);
            }

            mForm = (Form) fragments.findFragmentById(R.id.task_form);
            mList = (List) fragments.findFragmentById(R.id.tasks_list);
        }

        mTasks.query(Task.Mapper).watch(mShowTasks);
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        final boolean handled;

        switch (item.getItemId()) {
            case R.id.action_clear:
                mTasks.delete(where(Task.Finished).isEqualTo(true))
                        .onComplete(mShowDeleted);
                handled = true;
                break;
            default:
                handled = false;
        }

        return handled || super.onOptionsItemSelected(item);
    }

    @Override
    public final void save(final Task task) {
        mForm.disable();
        final Result<?> save = (mEditedTask == Task.NO_ID) ?
                mTasks.insert(task) :
                mDAO.at(TaskById, mEditedTask).update(task);
        save.onSomething(mClearForm).onNothing(mSaveError);
    }

    @Override
    public final void edit(final Task task) {
        mEditedTask = task.getId();
        mForm.edit(task);
    }

    @Override
    public final void open(final Task task) {
        mDAO.at(TaskById, task.getId()).update(false, Task.Finished);
    }

    @Override
    public final void close(final Task task) {
        mDAO.at(TaskById, task.getId()).update(true, Task.Finished);
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public final void onResume() {
        super.onResume();
        mDAO.start();
    }

    @Override
    public final void onPause() {
        mDAO.pause();
        super.onPause();
    }

    @Override
    public final void onDestroy() {
        mDAO.stop();
        mDAO = null;
        super.onDestroy();
    }

    @Override
    protected final void onSaveInstanceState(final Bundle state) {
        super.onSaveInstanceState(state);
        if (mEditedTask != Task.NO_ID) {
            state.putLong(EDITED_TASK_STATE, mEditedTask);
        }
    }
}
