event join {
    game `%default sneaks` = 0
    player.send("Sneak to increase ur counter xdd")
}

event sneak {
    `%default sneaks`++
    player.send("You now have " + `%default sneaks` + " sneaks")
}