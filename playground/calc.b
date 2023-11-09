// THE CALCULATOR IS CURRENTLY BROKEN AND DOES NOT WORK
local parserindex

event join {
	player.send($"<gray>To calculate, type a command like <white>@3+2<gray>.")
}

event command {
	input = event.arguments.join(" ")
	player.send("Input ", input)

    tokens = lexString(input)
    player.send("Tokens ", tokens)

    imul = addImplicitMul(tokens)
    player.send("Implicit Multiplication Tokens ", imul)

	tree = parseTokens(imul)
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
		{
			"name": "mod",
			"symbol": "%"
		},
		{
			"name": "oparen",
			"symbol": "("
		},
        {
			"name": "cparen",
			"symbol": ")"
		},
    ]
}

func lexString(tokens: var, source: string) {
    characters = source.split("")
    count = 0
    iter = 0
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
        if (character matches regex "[a-zA-Z]") {
            idString = ""
            repeat forever {
                character = characters[count]
                if !(character matches regex "[a-zA-Z]") {
                    stop
                }
                idString = string(idString, character)
                count++
                if (count >= listlen(characters)) {
                    stop
                }
            }
            tokens.add({
                "type": "id",
                "value": idString
            })
            count--
        }
        character = characters[count]
        symbols = getSymbols()
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
        iter++
        if (iter > 100) {
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
			if (operator != "imul") {
				if (operator != "div") {
                    stop
                }
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
	if (tokenindex["type"] == "id") {
	    parserindex++
	    tree = {
	        "type": "id",
	        "value": tokenindex["value"]
	    }
	    return
	}
	if (tokenindex["type"] == "oparen") {
	    parserindex++
	    expr = parseExpression(tokens)
	    tokenindex = tokens[parserindex]
	    if (tokenindex["type"] == "cparen") {
	        parserindex++
	        tree = expr
	        return
	    }

	    tree = {}
	    return
	}
	if (tokenindex["type"] == "sub") {
	    tokennext = tokens[parserindex + 1]
	    if (tokennext["type"] == "number") {
	        tokens.removeAt(index)
	        tokens.removeAt(index)
	        tokens.insert(index, {
	            "type": "num",
	            "value": 0 - tokennext["value"]
	        })

            tree = parseFactor(tokens)
	        return
	    }
	    if (tokennext["type"] == ["id", "oparen"]) {
	        tokens.insert(index, {
                "type": "num",
                "value": 0
            })

            tree = parseFactor(tokens)
            return
	    }
	}
}

func getIdentifiers(dst: var) {
    dst = [
        {
            "name": "pi",
            "value": 3.141
        },
        {
            "name": "apery",
            "value": 1.202
        },
        {
            "name": "e",
            "value": 2.718,
            "aliases": [ "euler" ]
        }
        {
            "name": "golden",
            "value": 1.618
        }
    ]
}

func evaluateTree(result: var, tree: dict) {
	if (tree["type"] == "number") {
		result = tree["value"]
		return
	}
	if (tree["type"] == "id") {
	    name = tree["value"]
	    value = 0
	    ids = getIdentifiers()
	    idIter = 0
	    repeat forever {
	        cId = ids[idIter]
	        if (cId["name"] == name) {
	            value = cId["value"]
	            stop
	        }
	        idIter++
	        if (idIter >= listlen(ids)) {
	            stop
	        }
	    }
	    result = value
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
	if (tree["type"] == ["mul", "imul"]) {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left * right
		return
	}
	if (tree["type"] == "div") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		player.send("division")
		result = left / right
		return
	}
	if (tree["type"] == "pow") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left ^ right
		return
	}
	if (tree["type"] == "mod") {
		left = evaluateTree(tree["left"])
		right = evaluateTree(tree["right"])
		result = left mod right
		return
	}
}

func addImplicitMul(newTokens: var, tokens: list) {
    tokenAmount = listlen(tokens)

    opTokenTypes = [
        "add", "sub", "mul", "div", "pow",
        "oparen", "cparen"
    ]
    valTokenTypes = [
        "num", "string"
    ]

    newTokens = []

    index = 0
    repeat forever {
        token = tokens[index]

        nextindex = index + 1
        if (tokenAmount <= nextindex) {
            newTokens.add(token)
            stop
        }
        nexttoken = tokens[nextindex]
        newTokens.add(token)

        addMul = 0
        if !(token["type"] in opTokenTypes) {
            if (nexttoken["type"] == "oparen") {
                addMul = 1
            }
            if (nexttoken["type"] == "id") {
                addMul = 1
            }
        }
        if (token["type"] == "id") {
            if !(nexttoken["type"] in opTokenTypes) {
                addMul = 1
            }
            if (nexttoken["type"] == "oparen") {
                addMul = 1
            }
        }
        if (token["type"] == "cparen") {
            if !(nexttoken["type"] in opTokenTypes) {
                addMul = 1
            }
            if (nexttoken["type"] == "oparen") {
                addMul = 1
            }
            if (nexttoken["type"] == "id") {
                addMul = 1
            }
        }
        if (token["type"] in valTokenTypes) {
            if (nexttoken["type"] in valTokenTypes) {
                addMul = 1
            }
        }

        if (addMul) {
            newTokens.add({
                "type": "imul",
                "value": "*"
            })
        }

        index++
        if (index >= tokenAmount) {
            stop
        }
    }
}