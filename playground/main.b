game `%default clicks`

event join {
    `%default clicks` = 0
    player.send($"Left-click <gray>to get more clicks")
}

event lc {
    `%default clicks`++
    player.send($"<gray>You now have <white>" + `%default clicks` + " <gray>clicks.")
}