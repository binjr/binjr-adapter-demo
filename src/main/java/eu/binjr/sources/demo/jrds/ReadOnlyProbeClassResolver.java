/*
 *    Copyright 2020 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.sources.demo.jrds;

import jrds.Probe;
import jrds.configuration.ProbeClassResolver;
import jrds.factories.ProbeBean;
import jrds.probe.IndexedProbe;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReadOnlyProbeClassResolver extends ProbeClassResolver {

    @ProbeBean({"port", "instance", "processName", "global", "hostagent", "commandLine"})
    public static class DummyProbe extends Probe<Object, Object> {

        @Getter
        @Setter
        private Integer port = 0;

        @Getter
        @Setter
        private String instance = "instance";

        @Getter
        @Setter
        private String processName = "processName";

        @Getter
        @Setter
        private String global = "global";

        @Getter
        @Setter
        private String hostagent = "hostagent";

        @Getter
        @Setter
        private String commandLine = "commandLine";

        @Override
        public Map<Object, Object> getNewSampleValues() {
            return Collections.emptyMap();
        }

        @Override
        public String getSourceType() {
            return "DummyProbe";
        }

        public Boolean configure() {
            return true;
        }

        public Boolean configure(String string) {
            return true;
        }

        public Boolean configure(List<Object> list) {
            return true;
        }

        public Boolean configure(String s1, String s2) {
            return true;
        }

        @Override
        public boolean checkStore() {
            return true;
        }
    }

    @ProbeBean({"index", "pattern"})
    public static class DummyProbeIndexed extends DummyProbe implements IndexedProbe {

        @Getter
        @Setter
        private String index = "NA";

        @Getter
        @Setter
        private String pattern = "pattern";

        @Override
        public String getIndexName() {
            return index;
        }

        @Override
        public Boolean configure(String index) {
            this.index = index;
            return true;
        }

        @Override
        public Boolean configure(List<Object> list) {
            if (list != null && !list.isEmpty()) {
                this.index = list.get(list.size()-1).toString();
            }
            return true;
        }

        @Override
        public Boolean configure(String index, String s2) {
            this.index = index;
            return true;
        }
    }

    public ReadOnlyProbeClassResolver(ClassLoader classLoader) {
        super(classLoader);
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Probe<?, ?>> getClassByName(String className) throws ClassNotFoundException {
        Class<? extends Probe<?, ?>> originalClass = (Class<? extends Probe<?, ?>>) classLoader.loadClass(className);
        if (IndexedProbe.class.isAssignableFrom(originalClass)) {
            return (Class<? extends Probe<?, ?>>) DummyProbeIndexed.class;
        } else {
            return (Class<? extends Probe<?, ?>>) DummyProbe.class;
        }
    }

}
