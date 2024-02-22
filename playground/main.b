func sayHi(name: string) {
    player.send("Hello " + name + "!")
}

event join {
    sayHi("world")
}
