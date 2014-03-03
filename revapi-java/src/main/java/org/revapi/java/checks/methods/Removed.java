/*
 * Copyright $year Lukas Krejci
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
 * limitations under the License
 */

package org.revapi.java.checks.methods;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;

import org.revapi.MatchReport;
import org.revapi.java.checks.AbstractJavaCheck;
import org.revapi.java.checks.Code;

/**
 * @author Lukas Krejci
 * @since 0.1
 */
public final class Removed extends AbstractJavaCheck {

    @Override
    protected void doVisitMethod(@Nullable ExecutableElement oldMethod, @Nullable ExecutableElement newMethod) {
        if (oldMethod != null && newMethod == null && isAccessible(oldMethod)) {
            pushActive(oldMethod, null);
        }
    }

    @Nullable
    @Override
    protected List<MatchReport.Problem> doEnd() {
        ActiveElements<ExecutableElement> methods = popIfActive();
        if (methods == null) {
            return null;
        }

        //TODO check if the method didn't move to supertype
        //TODO check if the method didn't override a method from superclass

        return Collections.singletonList(createProblem(Code.METHOD_REMOVED));
    }
}
