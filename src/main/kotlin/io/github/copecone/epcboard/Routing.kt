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

        for (board in boards) {
            h1 {
                +board.name
            }
        }
    }
}