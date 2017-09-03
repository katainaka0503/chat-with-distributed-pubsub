package streams

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}

import scala.collection.mutable

@Singleton
class ChatFlow @Inject()(implicit actorSystem: ActorSystem){

  def flow(topic: String) = {

    val mediator = DistributedPubSub.get(implicitly[ActorSystem]).mediator

    val sink = Flow
      .fromFunction((message: Message) => DistributedPubSubMediator.Publish(topic, message))
      .to(Sink.actorRef(mediator, false))

    val source = SubscriberSource(topic, mediator)

    Flow.fromSinkAndSource(sink, source)
  }
}

object SubscriberSource {
  def apply(topic: String, mediator: ActorRef)(implicit actorSystem: ActorSystem) = new SubscriberSource(topic, mediator)
}

class SubscriberSource(topic: String, mediator: ActorRef)(implicit actorSystem: ActorSystem) extends GraphStage[SourceShape[Message]] {
  val out = Outlet[Message]("subscriber.out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    val queue = mutable.Queue[Message]()
    var canPush: Boolean = false

    override def preStart(): Unit = {
      val stageActor = getStageActor { case (_, message) => {
        message match {
          case message@Message(_) =>
            queue.enqueue(message)
            if (canPush) {
              push(out, queue.dequeue)
            }
        }
      }
      }.ref

      mediator ! DistributedPubSubMediator.Subscribe(topic, stageActor)
    }

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (queue.nonEmpty) {
          push(out, queue.dequeue)
        } else {
          canPush = true
        }
      }
    })
  }

  override def shape: SourceShape[Message] = SourceShape(out)
}

case class Message(content: String)
