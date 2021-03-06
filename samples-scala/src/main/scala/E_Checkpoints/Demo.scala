// Copyright (c) Microsoft. All rights reserved.

package E_Checkpoints

import akka.stream.scaladsl.Sink
import com.microsoft.azure.reactiveeventhubs.EventHubsMessage
import com.microsoft.azure.reactiveeventhubs.ResumeOnError._
import com.microsoft.azure.reactiveeventhubs.scaladsl._
import com.microsoft.azure.reactiveeventhubs.SourceOptions

/** Retrieve messages from Event hub and save the current position
  * In case of restart the stream starts from where it left
  * (depending on the configuration)
  *
  * Note, the demo requires Cassandra, you can start an instance with Docker:
  * # docker run -ip 9042:9042 --rm cassandra
  */
object Demo extends App {

  val console = Sink.foreach[EventHubsMessage] {
    m ⇒ println(s"enqueued-time: ${m.received}, offset: ${m.offset}, payload: ${m.contentAsString}")
  }

  // Stream using checkpointing
  EventHub().source(SourceOptions().saveOffsets)
    .to(console)
    .run()
}
