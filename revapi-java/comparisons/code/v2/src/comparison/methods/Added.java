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
package comparison.methods;

public final class Added {

    public interface Iface {
        void newMethod();
    }

    public static abstract class Abstract {
        private void ignored() {}
        public abstract void newMethod();
    }

    public static class Ordinary extends Abstract {
        public void newMethod() {}

        public void ordinaryMethod1() {

        }

        public void ordinaryMethod2() {

        }

        public void ordinaryMethod3() {}
    }

    public void newMethod() {}
}
