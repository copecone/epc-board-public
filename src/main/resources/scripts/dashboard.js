function CreateBoard() {
    let selectBox = document.querySelector(".room_selector")
    let selection = selectBox.options[selectBox.selectedIndex].value;

    if (selection != null) {
        let data = {"boardName": "New Board", "roomID": parseInt(selection)}
        let info = {};

        fetch("/board/create", {
            method: "POST",
            body: JSON.stringify(data)
        })
            .then((response) => response.json())
            .then((result) => {
                let socket = new WebSocket(result.boardSocket);

                socket.addEventListener("open", (event) => {
                    socket.send(JSON.stringify({"op": 1}));
                })

                socket.addEventListener("message", (event) => {
                    console.log(event.data)
                })
        })
    } else {
        alert("Please Select Room!")
    }
}

document.addEventListener("DOMContentLoaded", (event) => {
    let selectBox = document.querySelector(".room_selector")

    /** @type {Element[]} */
    let initialChildren = []
    for (let i = 0; i < selectBox.children.length; i++) {
        let child = selectBox.children[i]
        initialChildren.push(child)
    }

    console.log(initialChildren)

    selectBox.addEventListener("mousedown", (event) => {
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
})