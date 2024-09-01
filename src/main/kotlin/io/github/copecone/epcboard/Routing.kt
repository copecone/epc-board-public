package io.github.copecone.epcboard

import kotlinx.html.*

fun HTML.mainDashboard() {
    head {
        title { +"Homepage" }
        script { src = "/scripts/dashboard.js"}
    }

    body {
        button {
            onClick = "CreateBoard()"
            +"Create New Board"
        }

        select(classes = "room_selector") {
            option { value = "null"; +"" }
        }

        div(classes = "board_list") {
            boards.forEach { (id, board) ->
                div(classes = "board") {
                    h1 {
                        +board.name
                    }

                    meta("board_id") {
                        this.content = "$id"
                    }
                }
            }
        }
    }
}