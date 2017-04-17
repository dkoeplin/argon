package argon.test
import org.scalatest.{FlatSpec, Matchers}
import org.virtualized._

class StructTests extends FlatSpec with Matchers {

  "StructTest" should "compile" in {
    class StructTest extends Test {
      import IR._

      @struct class MyStruct(x: Int, y: Int)

      @virtualize
      def main() {
        val array = Array.tabulate(32) { i =>
          MyStruct(i + 1, i - 1)
        }

        val x = array(10).x
        val y = random[MyStruct]

        val sum = array.reduce { (a, b) =>
          a + b
        }

        println("Expected: 11")
        println("Value: " + x)
        println("Random: " + y)
        println("Sum: " + sum)
      }
    }
    (new StructTest).main(Array.empty)
  }

}
