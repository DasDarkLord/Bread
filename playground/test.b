func ok(c: var) {
    c = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
}

event join {
    ok(a) // `a = ok()` gets converted to this
    b = a[3]
    player.send(b)
}