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

package android.orm.playground.annotation;

import android.orm.sql.column.Reference;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static android.orm.sql.column.Reference.Action.NoAction;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
public @interface ForeignKey {

    @NonNls @NonNull String[] childKey();

    @NonNull Class<?> parent();

    @NonNls @NonNull String[] parentKey() default {};

    @NonNull Reference.Action onDelete() default NoAction;

    @NonNull Reference.Action onUpdate() default NoAction;
}
