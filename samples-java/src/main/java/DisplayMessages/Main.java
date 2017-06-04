// Copyright (c) Microsoft. All rights reserved.

package DisplayMessages;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.reactiveeventhubs.MessageFromDevice;
import com.microsoft.azure.reactiveeventhubs.SourceOptions;
import com.microsoft.azure.reactiveeventhubs.javadsl.EventHub;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static java.lang.System.out;

/**
 * Retrieve messages from IoT hub and display the data in the console
 */
public class Main extends ReactiveStreamingApp
{
    static ObjectMapper jsonParser = new ObjectMapper();

    public static void main(String args[])
    {
        // Source retrieving messages from two IoT hub partitions (e.g. partition 0 and 3)
        int[] partitions = {0, 2};
        SourceOptions options = new SourceOptions().partitions(partitions);
        Source<MessageFromDevice, NotUsed> messagesFromTwoPartitions1 = new EventHub().source(options);

        // Same, but different syntax using one of the shortcuts
        Source<MessageFromDevice, NotUsed> messagesFromTwoPartitions2 = new EventHub().source(Arrays.asList(0, 3));

        // Source retrieving from all IoT hub partitions for the past 24 hours
        Source<MessageFromDevice, NotUsed> messages = new EventHub().source(Instant.now().minus(1, ChronoUnit.DAYS));

        messages
                .filter(m -> m.messageSchema().equals("temperature"))
                .map(m -> parseTemperature(m))
                .filter(x -> x != null && (x.value < 18 || x.value > 22))
                .to(console())
                .run(streamMaterializer);
    }

    public static Sink<Temperature, CompletionStage<Done>> console()
    {
        return Sink.foreach(m ->
        {
            if (m.value <= 18)
            {
                out.println("Device: " + m.deviceId + ": temperature too LOW: " + m.value);
            } else
            {
                out.println("Device: " + m.deviceId + ": temperature to HIGH: " + m.value);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static Temperature parseTemperature(MessageFromDevice m)
    {
        try
        {
            Map<String, Object> hash = jsonParser.readValue(m.contentAsString(), Map.class);
            Temperature t = new Temperature();
            t.value = Double.parseDouble(hash.get("value").toString());
            t.deviceId = m.deviceId();
            return t;
        } catch (Exception e)
        {
            return null;
        }
    }
}
