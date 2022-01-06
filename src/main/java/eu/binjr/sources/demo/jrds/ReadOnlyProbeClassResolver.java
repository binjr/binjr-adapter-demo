package eu.binjr.sources.demo.jrds;

import jrds.Probe;
import jrds.configuration.ProbeClassResolver;
import jrds.factories.ProbeBean;
import jrds.probe.IndexedProbe;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReadOnlyProbeClassResolver extends ProbeClassResolver {

    @ProbeBean({"port", "instance", "processName", "global", "hostagent", "commandLine"})
    public static class DummyProbe extends Probe<Object, Object> {
        private Integer port = 0;
        private String instance = "instance";
        private String processName = "processName";
        private String global = "global";
        private String hostagent = "hostagent";
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

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getInstance() {
            return instance;
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public String getGlobal() {
            return global;
        }

        public void setGlobal(String global) {
            this.global = global;
        }

        public String getHostagent() {
            return hostagent;
        }

        public void setHostagent(String hostagent) {
            this.hostagent = hostagent;
        }

        public String getCommandLine() {
            return commandLine;
        }

        public void setCommandLine(String commandLine) {
            this.commandLine = commandLine;
        }
    }

    @ProbeBean({"index", "pattern"})
    public static class DummyProbeIndexed extends DummyProbe implements IndexedProbe {
        private String index = "NA";
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

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
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
