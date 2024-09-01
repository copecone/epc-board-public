document.addEventListener("DOMContentLoaded", () => {
    let selectBox = document.querySelector(".room_selector")
    let boardList = document.querySelector(".board_list")

    function boardSocketConnection(socket) {
        socket.addEventListener("open", () => {
            socket.send(JSON.stringify({"op": 0}));
        })

        socket.addEventListener("message", (event) => {
            console.log(event.data)
        })
    }

    function connectBoards() {
        for (let i = 0; i < boardList.children.length; i++) {
            let boardObject = boardList.children[i]
            let boardID = boardObject.querySelector("meta[name=board_id]").content

            fetch(`/board/${boardID}`, {
                method: "GET"
            })
                .then((response) => response.json())
                .then((result) => {
                    let socket = new WebSocket(result.boardSocket);
                    boardSocketConnection(socket);
                })
        }
    }

    function registerSelectBoxEvent() {
        /** @type {Element[]} */
        let initialChildren = []
        for (let i = 0; i < selectBox.children.length; i++) {
            let child = selectBox.children[i]
            initialChildren.push(child)
        }

        console.log(initialChildren)

        selectBox.addEventListener("mousedown", () => {
            fetch("/api-proxy/rooms", {
                method: "GET",
                mode: "same-origin"
            })
                .then((response) => response.text())
                .then((body) => JSON.parse(body))
                .then((result) => {
                    for (let child of selectBox.children) {
                        child.remove()
                    }

                    for (let initialChild of initialChildren) {
                        selectBox.appendChild(initialChild)
                    }

                    result.forEach((room) => {
                        let selection = document.createElement("option")
                        selection.value = room.id
                        selection.innerText = room.name

                        selectBox.append(selection)
                    })
                })
        })
    }

    connectBoards()
    registerSelectBoxEvent()

    function CreateBoard() {
        let selectBox = document.querySelector(".room_selector")
        let selection = selectBox.options[selectBox.selectedIndex].value;

        if (selection != null) {
            let data = {"boardName": "New Board", "roomID": parseInt(selection)}

            fetch("/board/create", {
                method: "POST",
                body: JSON.stringify(data)
            })
                .then((response) => response.json())
                .then((result) => {
                    let socket = new WebSocket(result.boardSocket);
                    boardSocketConnection(socket)
                })
        } else {
            alert("Please Select Room!")
        }
    }
})