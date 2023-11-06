func fib(x: var, n: num) {
    if (n <= 1) {
        x = 1
    }
    if (n > 1) {
        y = fib(n - 1)
        z = fib(n - 2)
        x = y + z
    }
}

event join {
  repeat forever {
    fib(x, count++)
    player.send(x)
    wait()
  }
}