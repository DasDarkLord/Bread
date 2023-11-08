local parserindex

event join {
	player.send($"<gray>To calculate, type a command like <white>@3+2<gray>.")
}

event command {
	input = event.arguments.join(" ")
	player.send("Input ", input)

    tokens = lexString(input)
    player.send("Tokens ", tokens)
	
	tree = parseTokens(tokens)
	player.send("Tree ", tree)
	
	result = evaluateTree(tree)
	player.send("Result ", result)
}

func getSymbols(dst: var) {
    dst = [
        {
            "name": "add",
            "symbol": "+"
        },
		{
            "name": "sub",
            "symbol": "-"
        },
		{
            "name": "mul",
            "symbol": "*"
        },
		{
            "name": "div",
            "symbol": "/"
        },
		{
			"name": "pow",
			"symbol": "^"
		},
    ]
}

func lexString(tokens: var, source: string) {
    characters = source.split("")
    count = 0
    repeat forever {
        character = characters[count]
        if (character matches regex "[0-9]") {
			numberString = ""
            repeat forever {
                character = characters[count]
                if !(character matches regex "[0-9.]") {
                    stop
                }
                numberString = string(numberString, character)
                count++
                if (count >= listlen(characters)) {
                    stop
                }
            }
            tokens.add({
                "type": "num",
                "value": num(numberString)
            })
            count--
        }
        character = characters[count]
        symbols = getSymbols(s)
        symCount = 0
        repeat forever {
            symbol = symbols[symCount]
            if (symbol["symbol"] == character) {
                tokens.add({
                    "type": symbol["name"],
                    "value": symbol["symbol"]
                })
                count++
                stop
            }
            symCount++
            if (symCount >= listlen(symbols)) {
                stop
            }
        }
        if (count >= listlen(characters)) {
            stop
        }
    }
}

func parseTokens(tree: var, tokens: list) {
	parserindex = 0
	tree = parseExpression(tokens)
}

func parseExpression(tree: var, tokens: list) {
	left = parseTerm(tokens)
	
	repeat forever {
		if (parserindex >= listlen(tokens)) {
			stop
		}
		tokenindex = tokens[parserindex]
		operator = tokenindex["type"]
		if (operator != "add") {
			if (operator != "sub") {
				stop
			}
		}
		parserindex++
		right = parseTerm(tokens)
		left = {
			"type": operator,
			"left": left,
			"right": right
		}
	}
	
	tree = left
}

func parseTerm(tree: var, tokens: list) {
	left = parsePower(tokens)
	
	repeat forever {
		if (parserindex >= listlen(tokens)) {
			stop
		}
		tokenindex = tokens[parserindex]
		operator = tokenindex["type"]
		if (operator != "mul") {
			if (operator != "div") {
				stop
			}
		}
		parserindex++
		right = parsePower(tokens)
		left = {
			"type": operator,
			"left": left,
			"right": right
		}
	}
	
	tree = left
}

func parsePower(tree: var, tokens: list) {
	left = parseFactor(tokens)
	
	if (parserindex < listlen(tokens)) {
		indextoken = tokens[parserindex]
		if (indextoken["type"] == "pow") {
			operator = indextoken["type"]
			parserindex++
			right = parsePower(tokens)
			tree = {
				"type": operator,
				"left": left,
				"right": right
			}
			return
		}
	}
	
	tree = left	
}

func parseFactor(tree: var, tokens: list) {
	tokenindex = tokens[parserindex]
	if (tokenindex["type"] == "num") {
		parserindex++
		tree = {
			"type": "number",
			"value": tokenindex["value"]
		}
		return
	}
}

func evaluateTree(result: var, tree: dict) {
	if (tree["type"] == "number") {
		result = tree["value"]
		return
	}
	if (tree["type"] == "add") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left + right
		return 
	}
	if (tree["type"] == "sub") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left - right
		return 
	}
	if (tree["type"] == "mul") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left * right
		return 
	}
	if (tree["type"] == "div") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left / right
		return 
	}
	if (tree["type"] == "pow") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left ^ right
		return 
	}
}