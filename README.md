# Robosort

A [G-Earth](https://github.com/sirjonasxx/G-Earth) extension helping you to sort your wired stacks.

## Usage

There's two ways to use Robosort: sort on action and using commands.

### Sort on action

When this is enabled, the plugin will keep track of you adding, moving of removing wired boxes. When this is detected, it will automatically sort the stack(s) the box is placed on and/or removed from.

### Commands

When commands are enabled, you can use these command in the Habbo chat bar:

- `:sort` Activates sorting mode, after that just click on a box in the stack you want to sort.
- `:down [amount=1]` Click on a box in the stack you want to move down `[amount]` spaces.
- `:up [amount=1]` Click on a box in the stack you want to move down `[amount]` spaces.

> **Note:** `:down` and `:up` only move boxes of it's own type (e.g. a reaction box will only swap places with other reaction boxes). This is because this is a plugin for sorting the boxes based on type, so this is to keep it your stacks consistent.

## FAQ

Underneath are some frequently asked questions about Robosort.

### Will Robosort keep the current order of boxes when sorting?

**YES**. Robosort will use the current altitude of the boxes in the stack to determine the order of the boxes after sorting. This way, if you have a `WIRED Add-on: Execute In Order` box in the stack, the order of execution does not change. Robosort comes with the commands `:up` and `:down` to move boxes up and down in the stack if you require a different order.

### Can I keep building while Robosort is busy sorting?

**YES**. Just keep building, Robosort adds all planned movements to a queue so you can just keep building.

### Do I have to wait for Robosort to finish sorting a stack before I can change boxes in the stack?

**NO**. Robosort will keep track of all changes you make to the stack and will cancel any planned movements if you change the stack while it's busy sorting.

### Why is "Sort on action" not working?

First make sure it's enabled in the GUI of the plugin. If that's not the problem, enable commands and check the error you get when using the `:sort` command. It will tell you why Robosort is not sorting and how to fix it.

### Why are the `WIRED Text Add-On: Variable PlaceHolder` and `WIRED Text Add-On: Variable Capturer` are not sorted to the addons?

That's not a bug, it's a feature! Robosort makes these boxes part of the **effects** instead of the addons. The reason is that the value of the variables can change based on where these specific boxes are in the stack, if you have the `WIRED Add-on: Execute In Order` addon in the stack.

### How does Robosort do this without a stacktile?

Using the "Edit Variable" packet from the Wired Creator Tools. `WiredSetObjectVariableValue` to be exact. This does mean you need wired creator tools and wired edit privileges in a room to use Robosort. However, since you're sorting wired boxes, you probably have that already.