// See LICENSE for license details.

package firrtl_interpreter

package object executable {
  type Big = BigInt

  object Big {
    def apply(n: Int): Big = BigInt(n)
  }

  trait ExpressionResult

  type FuncInt  = () => Int
  type FuncLong = () => Long
  type FuncBig  = () => Big
  type FuncUnit = () => Unit

  trait Assigner {
    def run: FuncUnit
  }
}
