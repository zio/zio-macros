/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.macros

package object delegate {

  /**
   * Zoom into one trait that is part of composed trait and modify its implementation.
   */
  def patch[A, B](implicit ev: A Mix B): (B => B) => A with B => A with B =
    f => old => ev.mix(old, f(old))

  /**
   * Create a function that can be used to enrich ZIO or ZManaged with the mixed in trait instance.
   */
  def enrichWith[A](a: A): EnrichWith[A] = new EnrichWith(a)

  /**
   * Create a function that can be used to enrich ZIO or ZManaged with the mixed in effectfully created trait instance.
   * The generating effect can use the output of the effect that will be mixed into.
   */
  def enrichWithM[A]: EnrichWithM.PartiallyApplied[A] = new EnrichWithM.PartiallyApplied

  /**
   * Create a function that can be used to enrich ZIO or ZManaged with the mixed in effectfully created trait instance.
   * The generating effect can use the output of the effect that will be mixed into.
   */
  def enrichWithManaged[A]: EnrichWithManaged.PartiallyApplied[A] = new EnrichWithManaged.PartiallyApplied

}
