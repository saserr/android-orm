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

package android.orm.tasks.model;

import android.orm.Model;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.model.Property;
import android.orm.model.View;
import android.orm.sql.Column;
import android.orm.sql.Columns;
import android.orm.sql.Value;
import android.orm.util.Producer;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Columns.bool;
import static android.orm.sql.Columns.text;

public class Task extends Model {

    public static final long NO_ID = -1L;

    public static final Column<Long> Id = Columns.Id;
    public static final Column<String> Title = text("title").asNotNull();
    public static final Column<Boolean> Finished = bool("finished").asNotNull().withDefault(false);

    public static final Value.Constant Open = Finished.write(false);
    public static final Value.Constant Close = Finished.write(true);

    public static final Mapper.ReadWrite<Task> Mapper = Mappers.mapper(new Producer<Task>() {
        @NonNull
        @Override
        public Task produce() {
            return new Task();
        }
    });

    @NonNls
    private static final String NAME = "task";

    private final View<Long> mId = view(Id);
    private final Property<String> mTitle = property(Title);
    private final Property<Boolean> mFinished = property(Finished);

    private Task() {
        super(NAME);
    }

    public Task(final String title) {
        this();

        mTitle.set(title);
    }

    public final long getId() {
        final Long id = mId.getValue();
        return (id == null) ? NO_ID : id;
    }

    public final String getTitle() {
        return mTitle.getValue();
    }

    public final boolean isFinished() {
        final Boolean finished = mFinished.getValue();
        return (finished == null) ? false : finished;
    }

    @NonNls
    @Override
    public final String toString() {
        return "Task{" + "title=" + getTitle() + ", finished=" + isFinished() + '}';
    }
}
