event join {
    game %defaultSneaks = 0
    player.send("Sneak to increase ur counter xdd")
}

event sneak {
    %defaultSneaks++
    player.send("You now have " + %defaultSneaks + " sneaks")
}