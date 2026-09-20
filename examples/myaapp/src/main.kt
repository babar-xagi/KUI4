import ui4.*

fun main() = app {
    screen {
        center {
            column(gap = 16) {
                text("Hello Babar 👋", style = TextStyle.Headline)
                text("Running on KUI4 Pure Kotlin Platform! 🚀", style = TextStyle.Body)
            }
        }
    }
}
