package custodian

import akka.actor.ActorRef

import scala.annotation.implicitNotFound

/**
  * Created by dennis on 5/9/16.
  */
//@implicitNotFound(msg = "No implementation found for type [${E}, ${I}].")
//trait Position[E, I] {
//
//  def instrument(entry: E): I
//  def volume(entry: E): Long
//  def counterParty(entry: E): ActorRef
//
//}
//
//object Custodian {
//
//  def instrument[E, I](entry: E)(implicit ev: Position[E, I]) = ev.instrument(entry)
//  def volume[E, I](entry: E)(implicit ev: Position[E, I]) = ev.volume(entry)
//  def counterParty[E, I](entry: E)(implicit ev: Position[E, I]) = ev.counterParty(entry)
//
//}
