package com.rbkmoney.reporter.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "faulty-events")
@Data
public class FaultyEventsProperties {

    private List<Topic> topics;

    @Data
    public static class Topic {

        private String topicName;
        private List<Partition> partitions;

        @Data
        public static class Partition {

            private Integer partitionNumber;
            private List<Event> events;

            @Data
            public static class Event {
                private long offset;
            }
        }
    }

}


