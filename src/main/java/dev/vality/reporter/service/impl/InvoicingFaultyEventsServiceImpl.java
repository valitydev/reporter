package dev.vality.reporter.service.impl;

import dev.vality.reporter.config.properties.FaultyEventsProperties;
import dev.vality.reporter.model.KafkaEvent;
import dev.vality.reporter.service.FaultyEventsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoicingFaultyEventsServiceImpl implements FaultyEventsService {

    private final FaultyEventsProperties faultyEventsProperties;

    @Value("${kafka.topics.invoicing.id}")
    private String topicName;

    @Override
    public boolean isFaultyEvent(KafkaEvent event) {
        if (faultyEventsProperties.getTopics() == null) {
            return false;
        }

        FaultyEventsProperties.Topic faultyTopic = faultyEventsProperties.getTopics().stream()
                .filter(topic -> topicName.equals(topic.getTopicName()))
                .findFirst()
                .orElse(null);
        if (faultyTopic == null) {
            return false;
        }

        FaultyEventsProperties.Topic.Partition faultyPartition = faultyTopic.getPartitions().stream()
                .filter(partition -> partition.getPartitionNumber().equals(event.getPartition()))
                .findFirst()
                .orElse(null);
        if (faultyPartition == null) {
            return false;
        }

        return faultyPartition.getEvents().stream()
                .anyMatch(faultEvent -> faultEvent.getOffset() == event.getOffset());
    }

}
