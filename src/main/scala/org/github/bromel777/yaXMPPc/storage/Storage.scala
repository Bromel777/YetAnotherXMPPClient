package org.github.bromel777.yaXMPPc.storage

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.{Applicative, Monad}
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.logging._

import scala.collection.immutable.HashMap

trait Storage[F[_], K, V] {
  def put(key: K, value: V): F[Unit]
  def get(key: K): F[Option[V]]
  def find(func: (K, V) => Boolean): F[Option[(K, V)]]
  def contains(key: K): F[Boolean]
  def delete(key: K): F[Unit]
}

object Storage {

  def makeMapStorage[F[_]: Sync, K, V](implicit logs: Logs[F, F]): F[Storage[F, K, V]] =
    logs.forService[Storage[F, K, V]].flatMap { implicit logging =>
      Ref.of[F, HashMap[K, V]](HashMap.empty[K, V]).map { ref =>
        new Live(ref)
      }
    }

  final class Live[F[_]: Monad: Logging, K, V](map: Ref[F, HashMap[K, V]]) extends Storage[F, K, V] {

    override def put(key: K, value: V): F[Unit] =
      trace"Put ${key.toString} to storage" >> map.update(_ + (key -> value))

    override def get(key: K): F[Option[V]] =
      trace"Trying to get ${key.toString} from storage" >> map.get.map(_.get(key))

    override def contains(key: K): F[Boolean] =
      trace"Check if storage contains ${key.toString}" >> map.get.map(_.contains(key))

    override def delete(key: K): F[Unit] =
      trace"Delete key ${key.toString} from storage" >> map.update(_ - key)

    override def find(func: (K, V) => Boolean): F[Option[(K, V)]] =
      trace"Going to find key by func" >> map.get.map(_.find(func(_)))
  }
}
